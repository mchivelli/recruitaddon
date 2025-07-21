package com.yourname.myforgemod.client.gui;

import com.yourname.myforgemod.network.NetworkHandler;
import com.yourname.myforgemod.network.PacketMarchCommand;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MarchCoordinateScreen extends Screen {

    private final List<UUID> recruitUuids;
    private EditBox xCoordBox;
    private EditBox zCoordBox;

    public MarchCoordinateScreen(List<UUID> recruitUuids) {
        super(Component.literal("Set March Coordinates"));
        this.recruitUuids = recruitUuids;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.xCoordBox = new EditBox(this.font, centerX - 105, centerY - 10, 100, 20, Component.literal("X"));
        this.zCoordBox = new EditBox(this.font, centerX + 5, centerY - 10, 100, 20, Component.literal("Z"));

        addRenderableWidget(this.xCoordBox);
        addRenderableWidget(this.zCoordBox);

        addRenderableWidget(Button.builder(Component.literal("Confirm March"), button -> onConfirm())
            .pos(centerX - 100, centerY + 20)
            .size(200, 20)
            .build());
    }
    
    private void onConfirm() {
        try {
            int x = Integer.parseInt(xCoordBox.getValue());
            int z = Integer.parseInt(zCoordBox.getValue());
            NetworkHandler.sendToServer(new PacketMarchCommand(new BlockPos(x, 0, z), recruitUuids));
            this.minecraft.setScreen(null); // Close the screen
        } catch (NumberFormatException e) {
            // Handle invalid number format
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 40, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
} 