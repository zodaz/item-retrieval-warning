package com.itemretrievalwarning;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class ItemRetrievalWarningOverlay extends Overlay
{

    private Client client;
    private ItemRetrievalWarningPlugin plugin;
    private Color color;
    private boolean drawFlash = false;

    @Inject
    public ItemRetrievalWarningOverlay(Client client, ItemRetrievalWarningPlugin plugin)
    {
	this.color = new Color(255,0,0,40);
	this.client = client;
	this.plugin = plugin;
	setPosition(OverlayPosition.DYNAMIC);
	setMovable(true);
	setLayer(OverlayLayer.UNDER_WIDGETS);
    }

    public void tick()
    {
	drawFlash = !drawFlash;
    }

    @Override
    public Dimension render(final Graphics2D graphics)
    {
	if(plugin.isWarning() && drawFlash)
	{
	    graphics.setColor(color);
	    graphics.fillRect(0, 0,client.getCanvasWidth(), client.getCanvasHeight());
	}
	return null;
    }
}