package keystrokesmod.module.impl.combat.morekb;

import keystrokesmod.module.Module;

public abstract class IMoreKB extends Module {
    private boolean canSprint = true;
    private boolean sprintWasPressed = false;

    public IMoreKB(String name, category moduleCategory) {
        super(name, moduleCategory);
    }

    public void stopSprint() {
        canSprint = false;
        sprintWasPressed = mc.thePlayer.isSprinting() || mc.gameSettings.keyBindSprint.isKeyDown();
    }

    public void reSprint() {
        canSprint = true;
        if (sprintWasPressed && mc.thePlayer != null) {
            mc.thePlayer.setSprinting(true);
        }
        sprintWasPressed = false;
    }

    protected boolean noSprint() {
        return !canSprint;
    }

    protected boolean shouldReSprint() {
        return sprintWasPressed;
    }

    @Override
    public void onDisable() {
        canSprint = true;
        sprintWasPressed = false;
    }
}
