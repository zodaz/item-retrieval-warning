package com.itemretrievalwarning;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import java.awt.*;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
	name = "Item Retrieval Service Warning"
)
public class ItemRetrievalWarningPlugin extends Plugin
{
    private static final int[] SAFE_REGIONS = new int[]
    {
	//Barbarian assault
	7508, 7509, 10322,
	//Castle Wars
	9520, 9620,
	//Chambers of Xeric
	12889, 13136, 13137, 13138, 13139, 13140, 13141, 13145, 13393, 13394, 13395, 13396, 13397, 13401,
	//Clan Wars
	12621, 12622, 12623, 13130, 13131, 13133, 13134, 13135, 13386, 13387, 13390, 13641, 13642, 13643, 13644, 13645, 13646, 13647, 13899, 13900, 14155, 14156,
	//Emir's Arena
	13362,
	//Fishing Trawler
	7499,
	//Inferno
	9043, 9806, 10062,
	//Last man standing
	13658, 13659, 13660, 13914, 13915, 13916, 13918, 13919, 13920, 14174, 14175, 14176, 14430, 14431, 14432,
	//Mage Training Arena
	13462, 13463,
	//Nightmare Zone
	9033,
	//Pest Control
	10536,
	//POH
	7513, 7514, 7769, 7770,
	//Soul wars
	8493, 8749, 9005,
	//TzHaar Fight Cave
	9551,
	//TzHaar Fight Pit
	9552,
	//Rogues' Den
	11854, 11855, 12110, 12111
    };

    private final int ITEM_RETRIEVAL_GROUP_ID = 602;
    private final int ITEM_RETRIEVAL_CHILD_NAME = 1;
    private final int ITEM_RETRIEVAL_CHILD_ITEMS = 3;
    private final int KEPT_ON_DEATH_ITEM_RETRIEVAL = 3;

    private boolean itemRetrievalInterfaceOpen = false;
    private boolean syncKeptItemsInterface = false;
    private boolean isDeathBanked = false;
    private boolean isWarning = false;
    private boolean deathBankStatusKnown = false;

    private int lastGameState = -1;
    private int currentRegion = -1;

    private ReminderInfobox infoBox;

    @Inject private Client client;
    @Inject private OverlayManager overlayManager;
    @Inject private ItemRetrievalWarningOverlayPanel overlayPanel;
    @Inject private InfoBoxManager infoBoxManager;
    @Inject private ItemManager itemManager;
    @Inject private Notifier notifier;
    @Inject private ItemRetrievalWarningConfig config;
    @Inject private ItemRetrievalWarningOverlay overlay;

    @Provides ItemRetrievalWarningConfig provideConfig(ConfigManager configManager)
    {
	return configManager.getConfig(ItemRetrievalWarningConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
	infoBox = new ReminderInfobox(itemManager.getImage(964), this);
	infoBox.setTooltip("You have items stored in an item retrieveal service.");
	overlayManager.add(overlay);
	overlayManager.add(overlayPanel);
	confirmDeathbankStatusKnown(client.getGameState());
    }

    @Override
    protected void shutDown() throws Exception
    {
	infoBoxManager.removeInfoBox(infoBox);
	overlayManager.remove(overlay);
	overlayManager.remove(overlayPanel);
	infoBox.showing = false;
	itemRetrievalInterfaceOpen = false;
	isWarning = false;
	deathBankStatusKnown = false;
	setDeathBanked(false);
	lastGameState = -1;
    }

    @Subscribe
    protected void onChatMessage(ChatMessage chatMessage)
    {
	if(chatMessage.getType() == ChatMessageType.GAMEMESSAGE)
	{
	    if(chatMessage.getMessage().contains("retrieved some of your items") || chatMessage.getMessage().contains("items stored in an item retrieval service"))
		setDeathBanked(true);
	    else if(chatMessage.getMessage().contains("You have died again") && chatMessage.getMessage().contains("lost the items"))
		setDeathBanked(false);
	}
    }

    @Subscribe
    protected void onWidgetLoaded(WidgetLoaded loaded)
    {
	if (loaded.getGroupId() == ITEM_RETRIEVAL_GROUP_ID)
	{
	    itemRetrievalInterfaceOpen = true;
	    deathBankStatusKnown = true;
	    processItemRetrievalInterface();
	}
	else if(loaded.getGroupId() == WidgetID.KEPT_ON_DEATH_GROUP_ID)
	{
	    syncKeptItemsInterface = true;
	}
    }

    @Subscribe
    protected void onWidgetClosed(WidgetClosed closed)
    {
	if (closed.getGroupId() == ITEM_RETRIEVAL_GROUP_ID)
	    itemRetrievalInterfaceOpen = false;
    }

    @Subscribe
    protected void onGameTick(GameTick tick)
    {
	currentRegion =  WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();

	checkHealth();
	refreshInfobox();

	if(itemRetrievalInterfaceOpen)
	    processItemRetrievalInterface();
	if(syncKeptItemsInterface)
	    processKeptOnDeathInterface();
	if(isWarning)
	    overlay.tick();

	//Zulrah special case for UIM:s
	if (currentRegion == 8751)
	{
	    Widget npcDialog = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);
	    if (npcDialog != null)
	    {
		String message = npcDialog.getText().replace("<br>", " ");
		if (message.contains("You left some stuff at Zulrah's shrine when you left earlier") || message.contains("I've got some stuff you left at the shrine earlier"))
		    setDeathBanked(!message.contains("I've returned it to you now"));
		else if (message.contains("I don't have anything for you to collect"))
		    setDeathBanked(false);
	    }
	}
    }

    @Subscribe
    protected void onGameStateChanged(GameStateChanged gameStateChanged)
    {
	confirmDeathbankStatusKnown(gameStateChanged.getGameState());
	lastGameState = gameStateChanged.getGameState().getState();
    }

    private void confirmDeathbankStatusKnown(GameState currentGameState)
    {
	deathBankStatusKnown = !(currentGameState.getState() == GameState.LOGGED_IN.getState() && lastGameState == -1);
    }

    private void processItemRetrievalInterface()
    {
	int itemCount = getItemRetrievalItemCount();
	setDeathBanked(itemCount != 0);
    }

    public void setDeathBanked(boolean deathBanked)
    {
	isDeathBanked = deathBanked;
    }

    private void processKeptOnDeathInterface()
    {
	syncKeptItemsInterface = false;
	Widget widget = client.getWidget(WidgetID.KEPT_ON_DEATH_GROUP_ID, KEPT_ON_DEATH_ITEM_RETRIEVAL);
	if (widget != null && widget.getChildren() != null)
	{
	    for (Widget w : widget.getChildren())
	    {
		if (w.getType() == WidgetType.TEXT)
		{
		    if (w.getText().contains("die again") && w.getText().contains("before collecting") &&
			w.getText().contains("deleted"))
		    {
			deathBankStatusKnown = true;
			setDeathBanked(true);
			return;
		    }
		}
	    }
	}
	else
	{
	    deathBankStatusKnown = true;
	    setDeathBanked(false);
	}
    }

    private void checkHealth()
    {
	int hp = (int)(((double)client.getBoostedSkillLevel(Skill.HITPOINTS) / (double)client.getRealSkillLevel(Skill.HITPOINTS)) * 100);
	boolean prevIsWarningValue = isWarning;
	isWarning = (hp < config.warningThreshold() && riskingDeathBank());
	if(isWarning && !prevIsWarningValue)
	    notifier.notify("You are about to lose items you have stored in an item retrieval service!");
    }

    private void refreshInfobox()
    {
	if (infoBox.showing && (!isDeathBanked || !config.displayInfobox()))
	{
	    infoBoxManager.removeInfoBox(infoBox);
	    infoBox.showing = false;
	}
	else if(!infoBox.showing && (isDeathBanked && config.displayInfobox()))
	{
	    infoBoxManager.addInfoBox(infoBox);
	    infoBox.showing = true;
	}
    }

    private int getItemRetrievalItemCount()
    {
	int items = 0;
	Widget widget = client.getWidget(ITEM_RETRIEVAL_GROUP_ID, ITEM_RETRIEVAL_CHILD_ITEMS);

	if(widget != null)
	{
	    Widget[] widgets = widget.getChildren();
	    if(widgets != null)
	    {
		for (Widget w : widgets)
		    if (w != null && w.getItemId() != -1 && ! w.isHidden())
			items++;
	    }
	}
	return items;
    }

    public boolean isWarning()
    {
	return isWarning;
    }

    public boolean isDeathBankStatusKnown()
    {
	return deathBankStatusKnown;
    }

    private boolean riskingDeathBank()
    {
	for (int region : SAFE_REGIONS)
	    if (region == currentRegion)
		return false;
	return isDeathBanked;
    }

    private class ReminderInfobox extends InfoBox
    {
	private boolean showing;

	private ReminderInfobox(BufferedImage image, ItemRetrievalWarningPlugin plugin)
	{
	    super(image, plugin);
	    showing = false;
	}

	@Override public String getText() {
	    return "";
	}

	@Override public Color getTextColor() {
	    return Color.WHITE;
	}
    }
}