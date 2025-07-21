package com.yourname.myforgemod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourname.myforgemod.march.MarchManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class MarchGuiHandler extends Screen {
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 120;
    
    private EditBox xCoordInput;
    private EditBox zCoordInput;
    private Button confirmButton;
    private Button cancelButton;
    
    private final Player player;
    
    public MarchGuiHandler(Player player) {
        super(Component.translatable("gui.recruitsmarch.march.input.title"));
        this.player = player;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int guiLeft = centerX - GUI_WIDTH / 2;
        int guiTop = centerY - GUI_HEIGHT / 2;
        
        // X coordinate input
        this.xCoordInput = new EditBox(this.font, guiLeft + 20, guiTop + 40, 70, 20, 
            Component.translatable("gui.recruitsmarch.march.input.x"));
        this.xCoordInput.setMaxLength(10);
        this.xCoordInput.setValue(String.valueOf((int) player.getX()));
        this.addWidget(this.xCoordInput);
        
        // Z coordinate input
        this.zCoordInput = new EditBox(this.font, guiLeft + 110, guiTop + 40, 70, 20, 
            Component.translatable("gui.recruitsmarch.march.input.z"));
        this.zCoordInput.setMaxLength(10);
        this.zCoordInput.setValue(String.valueOf((int) player.getZ()));
        this.addWidget(this.zCoordInput);
        
        // Confirm button
        this.confirmButton = Button.builder(
            Component.translatable("gui.recruitsmarch.march.input.confirm"),
            this::onConfirmPressed
        ).bounds(guiLeft + 20, guiTop + 80, 70, 20).build();
        this.addRenderableWidget(this.confirmButton);
        
        // Cancel button
        this.cancelButton = Button.builder(
            Component.translatable("gui.recruitsmarch.march.input.cancel"),
            this::onCancelPressed
        ).bounds(guiLeft + 110, guiTop + 80, 70, 20).build();
        this.addRenderableWidget(this.cancelButton);
        
        this.setInitialFocus(this.xCoordInput);
    }
    
    private void onConfirmPressed(Button button) {
        try {
            int x = Integer.parseInt(this.xCoordInput.getValue());
            int z = Integer.parseInt(this.zCoordInput.getValue());
            
            BlockPos targetPos = new BlockPos(x, player.getBlockY(), z);
            
            // Start the march
            boolean success = MarchManager.startMarch(player, targetPos);
            
            if (success) {
                player.sendSystemMessage(Component.literal("§aRecruits are marching to coordinates " + x + ", " + z + "!"));
            } else {
                player.sendSystemMessage(Component.literal("§cNo recruits found to march! Make sure you have recruits nearby."));
            }
            
            this.onClose();
            
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("§cInvalid coordinates! Please enter valid numbers."));
        }
    }
    
    private void onCancelPressed(Button button) {
        this.onClose();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int guiLeft = centerX - GUI_WIDTH / 2;
        int guiTop = centerY - GUI_HEIGHT / 2;
        
        // Draw background panel
        guiGraphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0x80000000);
        guiGraphics.fill(guiLeft + 1, guiTop + 1, guiLeft + GUI_WIDTH - 1, guiTop + GUI_HEIGHT - 1, 0xFF404040);
        
        // Draw title
        Component title = Component.translatable("gui.recruitsmarch.march.input.title");
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, centerX - titleWidth / 2, guiTop + 10, 0xFFFFFF);
        
        // Draw labels
        Component xLabel = Component.translatable("gui.recruitsmarch.march.input.x");
        Component zLabel = Component.translatable("gui.recruitsmarch.march.input.z");
        
        guiGraphics.drawString(this.font, xLabel, guiLeft + 20, guiTop + 30, 0xFFFFFF);
        guiGraphics.drawString(this.font, zLabel, guiLeft + 110, guiTop + 30, 0xFFFFFF);
        
        // Render input fields
        this.xCoordInput.render(guiGraphics, mouseX, mouseY, partialTick);
        this.zCoordInput.render(guiGraphics, mouseX, mouseY, partialTick);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC key
            this.onClose();
            return true;
        } else if (keyCode == 257 || keyCode == 335) { // ENTER or NUMPAD_ENTER
            this.onConfirmPressed(this.confirmButton);
            return true;
        }
        
        return this.xCoordInput.keyPressed(keyCode, scanCode, modifiers) ||
               this.zCoordInput.keyPressed(keyCode, scanCode, modifiers) ||
               super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.xCoordInput.charTyped(codePoint, modifiers) ||
               this.zCoordInput.charTyped(codePoint, modifiers) ||
               super.charTyped(codePoint, modifiers);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.xCoordInput.mouseClicked(mouseX, mouseY, button) ||
               this.zCoordInput.mouseClicked(mouseX, mouseY, button) ||
               super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
} 