package com.yourname.myforgemod.client.gui;


import com.yourname.myforgemod.integration.RecruitsIntegration;
import com.yourname.myforgemod.network.NetworkHandler;
import com.yourname.myforgemod.network.PacketRecruitMovement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.minecraft.world.phys.Vec3;

/**
 * GUI screen for controlling recruit movement
 */
public class RecruitMovementScreen extends Screen {

    
    private EditBox xCoordBox;
    private EditBox yCoordBox;
    private EditBox zCoordBox;
    private EditBox groupBox;
    
    private Button moveAllButton;
    private Button moveGroupButton;
    private Button marchAllButton;
    private Button marchGroupButton;
    private Button recallAllButton;
    private Button recallGroupButton;
    private Button holdAllButton;
    private Button holdGroupButton;
    private Button followAllButton;
    private Button followGroupButton;
    private Button stopAllButton;
    private Button stopGroupButton;
    private Button statusButton;
    
    private Button formationLineButton;
    private Button formationSquareButton;
    private Button formationCircleButton;
    private Button formationWedgeButton;
    
    private RecruitsIntegration.FormationType selectedFormation = RecruitsIntegration.FormationType.LINE;
    
    public RecruitMovementScreen() {
        super(Component.literal("Recruit Movement Control"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Coordinate input boxes
        this.xCoordBox = new EditBox(this.font, centerX - 150, centerY - 80, 60, 20, Component.literal("X"));
        this.yCoordBox = new EditBox(this.font, centerX - 80, centerY - 80, 60, 20, Component.literal("Y"));
        this.zCoordBox = new EditBox(this.font, centerX - 10, centerY - 80, 60, 20, Component.literal("Z"));
        this.groupBox = new EditBox(this.font, centerX + 70, centerY - 80, 60, 20, Component.literal("Group"));
        
        this.xCoordBox.setMaxLength(10);
        this.yCoordBox.setMaxLength(10);
        this.zCoordBox.setMaxLength(10);
        this.groupBox.setMaxLength(3);
        
        this.xCoordBox.setValue("0");
        this.yCoordBox.setValue("64");
        this.zCoordBox.setValue("0");
        this.groupBox.setValue("0");
        
        this.addRenderableWidget(this.xCoordBox);
        this.addRenderableWidget(this.yCoordBox);
        this.addRenderableWidget(this.zCoordBox);
        this.addRenderableWidget(this.groupBox);
        
        // Formation buttons
        this.formationLineButton = Button.builder(Component.literal("Line"), button -> {
            this.selectedFormation = RecruitsIntegration.FormationType.LINE;
            updateFormationButtons();
        }).bounds(centerX - 150, centerY - 50, 60, 20).build();
        
        this.formationSquareButton = Button.builder(Component.literal("Square"), button -> {
            this.selectedFormation = RecruitsIntegration.FormationType.SQUARE;
            updateFormationButtons();
        }).bounds(centerX - 80, centerY - 50, 60, 20).build();
        
        this.formationCircleButton = Button.builder(Component.literal("Circle"), button -> {
            this.selectedFormation = RecruitsIntegration.FormationType.CIRCLE;
            updateFormationButtons();
        }).bounds(centerX - 10, centerY - 50, 60, 20).build();
        
        this.formationWedgeButton = Button.builder(Component.literal("Wedge"), button -> {
            this.selectedFormation = RecruitsIntegration.FormationType.WEDGE;
            updateFormationButtons();
        }).bounds(centerX + 60, centerY - 50, 60, 20).build();
        
        this.addRenderableWidget(this.formationLineButton);
        this.addRenderableWidget(this.formationSquareButton);
        this.addRenderableWidget(this.formationCircleButton);
        this.addRenderableWidget(this.formationWedgeButton);
        
        // Movement buttons
        this.moveAllButton = Button.builder(Component.literal("Move All"), button -> {
            Vec3 coords = getCoordinates();
            if (coords != null) {
                NetworkHandler.sendToServer(new PacketRecruitMovement(
                    PacketRecruitMovement.Action.MOVE_ALL, coords, -1, selectedFormation));
            }
        }).bounds(centerX - 150, centerY - 20, 70, 20).build();
        
        this.moveGroupButton = Button.builder(Component.literal("Move Group"), button -> {
            Vec3 coords = getCoordinates();
            int group = getGroup();
            if (coords != null && group >= 0) {
                NetworkHandler.sendToServer(new PacketRecruitMovement(
                    PacketRecruitMovement.Action.MOVE_GROUP, coords, group, selectedFormation));
            }
        }).bounds(centerX - 70, centerY - 20, 70, 20).build();
        
        this.marchAllButton = Button.builder(Component.literal("March All"), button -> {
            Vec3 coords = getCoordinates();
            if (coords != null) {
                NetworkHandler.sendToServer(new PacketRecruitMovement(
                    PacketRecruitMovement.Action.MARCH_ALL, coords, -1, selectedFormation));
            }
        }).bounds(centerX + 10, centerY - 20, 70, 20).build();
        
        this.marchGroupButton = Button.builder(Component.literal("March Group"), button -> {
            Vec3 coords = getCoordinates();
            int group = getGroup();
            if (coords != null && group >= 0) {
                NetworkHandler.sendToServer(new PacketRecruitMovement(
                    PacketRecruitMovement.Action.MARCH_GROUP, coords, group, selectedFormation));
            }
        }).bounds(centerX + 90, centerY - 20, 70, 20).build();
        
        this.addRenderableWidget(this.moveAllButton);
        this.addRenderableWidget(this.moveGroupButton);
        this.addRenderableWidget(this.marchAllButton);
        this.addRenderableWidget(this.marchGroupButton);
        
        // Control buttons
        this.recallAllButton = Button.builder(Component.literal("Recall All"), button -> {
            NetworkHandler.sendToServer(new PacketRecruitMovement(
                PacketRecruitMovement.Action.RECALL_ALL, Vec3.ZERO, -1, selectedFormation));
        }).bounds(centerX - 150, centerY + 10, 70, 20).build();
        
        this.recallGroupButton = Button.builder(Component.literal("Recall Group"), button -> {
            int group = getGroup();
            if (group >= 0) {
                NetworkHandler.sendToServer(new PacketRecruitMovement(
                    PacketRecruitMovement.Action.RECALL_GROUP, Vec3.ZERO, group, selectedFormation));
            }
        }).bounds(centerX - 70, centerY + 10, 70, 20).build();
        
        this.holdAllButton = Button.builder(Component.literal("Hold All"), button -> {
            NetworkHandler.sendToServer(new PacketRecruitMovement(
                PacketRecruitMovement.Action.HOLD_ALL, Vec3.ZERO, -1, selectedFormation));
        }).bounds(centerX + 10, centerY + 10, 70, 20).build();
        
        this.holdGroupButton = Button.builder(Component.literal("Hold Group"), button -> {
            int group = getGroup();
            if (group >= 0) {
                NetworkHandler.sendToServer(new PacketRecruitMovement(
                    PacketRecruitMovement.Action.HOLD_GROUP, Vec3.ZERO, group, selectedFormation));
            }
        }).bounds(centerX + 90, centerY + 10, 70, 20).build();
        
        this.addRenderableWidget(this.recallAllButton);
        this.addRenderableWidget(this.recallGroupButton);
        this.addRenderableWidget(this.holdAllButton);
        this.addRenderableWidget(this.holdGroupButton);
        
        // Additional control buttons
        this.followAllButton = Button.builder(Component.literal("Follow All"), button -> {
            NetworkHandler.sendToServer(new PacketRecruitMovement(
                PacketRecruitMovement.Action.FOLLOW_ALL, Vec3.ZERO, -1, selectedFormation));
        }).bounds(centerX - 150, centerY + 40, 70, 20).build();
        
        this.followGroupButton = Button.builder(Component.literal("Follow Group"), button -> {
            int group = getGroup();
            if (group >= 0) {
                NetworkHandler.sendToServer(new PacketRecruitMovement(
                    PacketRecruitMovement.Action.FOLLOW_GROUP, Vec3.ZERO, group, selectedFormation));
            }
        }).bounds(centerX - 70, centerY + 40, 70, 20).build();
        
        this.stopAllButton = Button.builder(Component.literal("Stop All"), button -> {
            NetworkHandler.sendToServer(new PacketRecruitMovement(
                PacketRecruitMovement.Action.STOP_ALL, Vec3.ZERO, -1, selectedFormation));
        }).bounds(centerX + 10, centerY + 40, 70, 20).build();
        
        this.stopGroupButton = Button.builder(Component.literal("Stop Group"), button -> {
            int group = getGroup();
            if (group >= 0) {
                NetworkHandler.sendToServer(new PacketRecruitMovement(
                    PacketRecruitMovement.Action.STOP_GROUP, Vec3.ZERO, group, selectedFormation));
            }
        }).bounds(centerX + 90, centerY + 40, 70, 20).build();
        
        this.addRenderableWidget(this.followAllButton);
        this.addRenderableWidget(this.followGroupButton);
        this.addRenderableWidget(this.stopAllButton);
        this.addRenderableWidget(this.stopGroupButton);
        
        // Status button
        this.statusButton = Button.builder(Component.literal("Get Status"), button -> {
            NetworkHandler.sendToServer(new PacketRecruitMovement(
                PacketRecruitMovement.Action.STATUS, Vec3.ZERO, -1, selectedFormation));
        }).bounds(centerX - 40, centerY + 70, 80, 20).build();
        
        this.addRenderableWidget(this.statusButton);
        
        // Close button
        Button closeButton = Button.builder(Component.literal("Close"), button -> {
            this.onClose();
        }).bounds(centerX - 40, centerY + 100, 80, 20).build();
        
        this.addRenderableWidget(closeButton);
        
        updateFormationButtons();
    }
    
    private void updateFormationButtons() {
        this.formationLineButton.active = (selectedFormation != RecruitsIntegration.FormationType.LINE);
        this.formationSquareButton.active = (selectedFormation != RecruitsIntegration.FormationType.SQUARE);
        this.formationCircleButton.active = (selectedFormation != RecruitsIntegration.FormationType.CIRCLE);
        this.formationWedgeButton.active = (selectedFormation != RecruitsIntegration.FormationType.WEDGE);
    }
    
    private Vec3 getCoordinates() {
        try {
            double x = Double.parseDouble(this.xCoordBox.getValue());
            double y = Double.parseDouble(this.yCoordBox.getValue());
            double z = Double.parseDouble(this.zCoordBox.getValue());
            return new Vec3(x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private int getGroup() {
        try {
            return Integer.parseInt(this.groupBox.getValue());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    @Override
    public void render(@javax.annotation.Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // Draw title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Draw labels
        guiGraphics.drawString(this.font, "X:", this.width / 2 - 170, this.height / 2 - 75, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Y:", this.width / 2 - 100, this.height / 2 - 75, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Z:", this.width / 2 - 30, this.height / 2 - 75, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Group:", this.width / 2 + 40, this.height / 2 - 75, 0xFFFFFF);
        
        // Draw formation label
        guiGraphics.drawString(this.font, "Formation:", this.width / 2 - 170, this.height / 2 - 45, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Selected: " + selectedFormation.name(), this.width / 2 + 140, this.height / 2 - 45, 0x00FF00);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
