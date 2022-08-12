package com.itemretrievalwarning;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.awt.*;

public class ItemRetrievalWarningOverlayPanel extends OverlayPanel
{
    private final ItemRetrievalWarningPlugin plugin;

    @Inject
    private ItemRetrievalWarningOverlayPanel(ItemRetrievalWarningPlugin plugin)
    {
	super(plugin);
	this.plugin = plugin;
	setPosition(OverlayPosition.TOP_LEFT);
	setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
	panelComponent.getChildren().clear();

	if(plugin.isWarning())
	{
	    LineComponent l = LineComponent.builder().left("You are about to lose items you have stored in an item retrieval service!").build();
	    panelComponent.getChildren().add(l);
	}
	else if (!plugin.isDeathBankStatusKnown())
	{
	    LineComponent l = LineComponent.builder().left("Unable to determine item retrieval service status. Open the items kept on " +
						     "death interface or hop worlds to determine status.").build();
	    panelComponent.getChildren().add(l);
	}

	return super.render(graphics);
    }

}