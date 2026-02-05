package keystrokesmod.module.impl.combat;

import keystrokesmod.event.MoveInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.SprintEvent;
import keystrokesmod.mixins.impl.client.KeyBindingAccessor;
import keystrokesmod.module.impl.combat.morekb.IMoreKB;
import keystrokesmod.module.impl.combat.morekb.SimpleSprintReset;
import keystrokesmod.module.setting.impl.ModeValue;
import keystrokesmod.utility.MoveUtil;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class MoreKB extends IMoreKB {
    private final ModeValue mode;
    
    // STap state
    private boolean sTapActive;
    private long sTapStartTime;
    private static final int S_TAP_DURATION_MS = 80; // ~4 ticks
    private static final int S_TAP_RECOVERY_MS = 60; // ~3 ticks

    public MoreKB() {
        super("MoreKB", category.combat);
        this.registerSetting(mode = new ModeValue("Mode", this)
                .add(new SimpleSprintReset("Legit", this))
                .add(new SimpleSprintReset("LegitSneak", this))
                .add(new SimpleSprintReset("LegitFast", this))
                .add(new SimpleSprintReset("Fast", this))
                .add(new SimpleSprintReset("Packet", this))
                .add(new SimpleSprintReset("LegitBlock", this))
                .add(new SimpleSprintReset("LegitInv", this))
                .add(new SimpleSprintReset("STap", this))
                .setDefaultValue("LegitFast")
        );
    }

    @Override
    public void onEnable() throws Exception {
        mode.enable();
        sTapActive = false;
        sTapStartTime = 0;
    }

    @Override
    public void onDisable() throws Exception {
        mode.disable();
        releaseSTap();
    }

    @SubscribeEvent
    public void onMoveInput(MoveInputEvent event) {
        if (!noSprint() || !MoveUtil.isMoving()) return;
        
        int currentMode = (int) mode.getInput();
        
        // Handle STap active state
        if (currentMode == 7 && sTapActive) {
            long elapsed = System.currentTimeMillis() - sTapStartTime;
            
            if (elapsed < S_TAP_DURATION_MS) {
                // Active STap: hold back, zero forward
                event.setForward(0f);
                ((KeyBindingAccessor) mc.gameSettings.keyBindBack).setPressed(true);
                ((KeyBindingAccessor) mc.gameSettings.keyBindForward).setPressed(false);
                return;
            } else if (elapsed < S_TAP_DURATION_MS + S_TAP_RECOVERY_MS) {
                // Recovery: neutral
                event.setForward(0f);
                ((KeyBindingAccessor) mc.gameSettings.keyBindBack).setPressed(false);
                return;
            } else {
                releaseSTap();
            }
        }
        
        // Standard modes
        switch (currentMode) {
            case 1:
                event.setSneak(true);
                break;
            case 3:
                event.setForward(0.7999f);
                break;
        }
    }

    @SubscribeEvent
    public void onSprint(SprintEvent event) {
        if (!noSprint() || !MoveUtil.isMoving()) return;
        
        int currentMode = (int) mode.getInput();
        
        // Suppress sprint during STap active phase
        if (currentMode == 7 && sTapActive) {
            long elapsed = System.currentTimeMillis() - sTapStartTime;
            if (elapsed < S_TAP_DURATION_MS + 50) {
                event.setSprint(false);
            }
        } else if (currentMode == 2) {
            event.setSprint(false);
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (!noSprint() || !MoveUtil.isMoving()) return;
        
        int currentMode = (int) mode.getInput();
        
        // Packet suppression during STap
        if (currentMode == 7 && sTapActive) {
            long elapsed = System.currentTimeMillis() - sTapStartTime;
            if (elapsed < S_TAP_DURATION_MS) {
                event.setSprinting(false);
            }
        } else if (currentMode == 4) {
            event.setSprinting(false);
        }
    }

    @Override
    public void stopSprint() {
        super.stopSprint();
        
        int currentMode = (int) mode.getInput();
        
        switch (currentMode) {
            case 0: // Legit - release forward
                ((KeyBindingAccessor) mc.gameSettings.keyBindForward).setPressed(false);
                break;
            case 5: // LegitBlock
                Utils.sendClick(1, true);
                break;
            case 6: // LegitInv
                ((KeyBindingAccessor) mc.gameSettings.keyBindInventory).setPressed(true);
                KeyBinding.onTick(mc.gameSettings.keyBindInventory.getKeyCode());
                ((KeyBindingAccessor) mc.gameSettings.keyBindInventory).setPressed(false);
                KeyBinding.onTick(mc.gameSettings.keyBindInventory.getKeyCode());
                break;
            case 7: // STap - start backwards movement
                startSTap();
                break;
        }
    }

    @Override
    public void reSprint() {
        super.reSprint();
        
        int currentMode = (int) mode.getInput();
        
        switch (currentMode) {
            case 0:
                ((KeyBindingAccessor) mc.gameSettings.keyBindForward).setPressed(
                    Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())
                );
                break;
            case 5:
                Utils.sendClick(1, false);
                break;
            case 6:
                if (mc.currentScreen instanceof GuiInventory)
                    mc.thePlayer.closeScreen();
                break;
            case 7:
                // STap handles re-engagement via time
                break;
        }
    }
    
    private void startSTap() {
        if (sTapActive) return;
        
        sTapActive = true;
        sTapStartTime = System.currentTimeMillis();
        
        // Immediate key presses
        ((KeyBindingAccessor) mc.gameSettings.keyBindBack).setPressed(true);
        ((KeyBindingAccessor) mc.gameSettings.keyBindForward).setPressed(false);
        
        // Ensure sprint is broken for WTap compatibility
        if (mc.thePlayer.isSprinting()) {
            mc.thePlayer.setSprinting(false);
            mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(
                mc.thePlayer, 
                C0BPacketEntityAction.Action.STOP_SPRINTING
            ));
        }
    }
    
    private void releaseSTap() {
        sTapActive = false;
        ((KeyBindingAccessor) mc.gameSettings.keyBindBack).setPressed(false);
        
        // Restore forward based on actual key state
        boolean forwardDown = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
        ((KeyBindingAccessor) mc.gameSettings.keyBindForward).setPressed(forwardDown);
        
        // Re-engage sprint if forward is held
        if (forwardDown && !mc.thePlayer.isSprinting()) {
            mc.thePlayer.setSprinting(true);
        }
    }
    
    public boolean isSTapActive() {
        return sTapActive;
    }

    @Override
    public String getInfo() {
        return mode.getSelected().getPrettyName();
    }
}
