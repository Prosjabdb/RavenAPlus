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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class MoreKB extends IMoreKB {
    private final ModeValue mode;

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
                .add(new SimpleSprintReset("STap", this))           // Proper STap
                .add(new SimpleSprintReset("STapFast", this))       // Fast STap variant
                .add(new SimpleSprintReset("STapPacket", this))     // Packet STap variant
                .setDefaultValue("LegitFast")
        );
    }

    @Override
    public void onEnable() throws Exception {
        mode.enable();
    }

    @Override
    public void onDisable() throws Exception {
        mode.disable();
    }

    @SubscribeEvent
    public void onMoveInput(MoveInputEvent event) {
        if (noSprint() && MoveUtil.isMoving()) {
            String modeName = mode.getSelected().getName().toLowerCase();
            int modeIndex = (int) mode.getInput();
            
            switch (modeIndex) {
                case 1: // LegitSneak
                    event.setSneak(true);
                    break;
                case 3: // Fast
                    event.setForward(0.7999f);
                    break;
                case 7: // STap
                case 8: // STapFast
                    // STap: Stop forward, press back + strafe
                    event.setForward(0.0f);
                    event.setBack(0.8f);
                    // Add slight strafe for unpredictability
                    if (mc.thePlayer.ticksExisted % 2 == 0) {
                        event.setLeft(0.3f);
                    } else {
                        event.setRight(0.3f);
                    }
                    break;
                case 9: // STapPacket - handled in onPreMotion
                    event.setForward(0.0f);
                    break;
            }
        }
    }

    @SubscribeEvent
    public void onSprint(SprintEvent event) {
        if (noSprint() && MoveUtil.isMoving() && (int) mode.getInput() == 2) {
            event.setSprint(false);
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (noSprint() && MoveUtil.isMoving()) {
            int modeIndex = (int) mode.getInput();
            if (modeIndex == 4 || modeIndex == 9) { // Packet or STapPacket
                event.setSprinting(false);
            }
        }
    }

    @Override
    public void stopSprint() {
        super.stopSprint();
        String modeName = mode.getSelected().getName().toLowerCase();
        int modeIndex = (int) mode.getInput();
        
        switch (modeIndex) {
            case 7: // STap
            case 8: // STapFast
            case 9: // STapPacket
                // STap: Press back key, release forward
                ((KeyBindingAccessor) mc.gameSettings.keyBindBack).setPressed(true);
                ((KeyBindingAccessor) mc.gameSettings.keyBindForward).setPressed(false);
                break;
            case 0: // Legit
            case 2: // LegitFast
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
        }
    }

    @Override
    public void reSprint() {
        super.reSprint();
        String modeName = mode.getSelected().getName().toLowerCase();
        int modeIndex = (int) mode.getInput();
        
        switch (modeIndex) {
            case 7: // STap
            case 8: // STapFast
            case 9: // STapPacket
                // Restore back key to actual keyboard state, restore forward
                ((KeyBindingAccessor) mc.gameSettings.keyBindBack).setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()));
                ((KeyBindingAccessor) mc.gameSettings.keyBindForward).setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
                break;
            case 0: // Legit
            case 2: // LegitFast
                ((KeyBindingAccessor) mc.gameSettings.keyBindForward).setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
                break;
            case 5: // LegitBlock
                Utils.sendClick(1, false);
                break;
            case 6: // LegitInv
                if (mc.currentScreen instanceof GuiInventory)
                    mc.thePlayer.closeScreen();
                break;
        }
    }

    @Override
    public String getInfo() {
        return mode.getSelected().getPrettyName();
    }
}
