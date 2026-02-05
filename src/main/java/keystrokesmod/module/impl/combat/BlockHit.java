package keystrokesmod.module.impl.combat;

import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.CoolDown;
import keystrokesmod.utility.Utils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.concurrent.ThreadLocalRandom;

public class BlockHit extends Module {
    public static SliderSetting range, chance, waitMsMin, waitMsMax, hitPerMin, hitPerMax, postDelayMin, postDelayMax;
    public static ModeSetting eventType;
    public static ButtonSetting onlyPlayers, onRightMBHold;
    
    private boolean executingAction, hitCoolDown, alreadyHit, safeGuard, waitingForPostDelay;
    private int hitTimeout, hitsWaited;
    private final CoolDown actionTimer = new CoolDown(0);
    private final CoolDown postDelayTimer = new CoolDown(0);

    public BlockHit() {
        super("BlockHit", category.combat, "Automatically blockHit");
        this.registerSetting(onlyPlayers = new ButtonSetting("Only combo players", true));
        this.registerSetting(onRightMBHold = new ButtonSetting("When holding down rmb", true));
        this.registerSetting(waitMsMin = new SliderSetting("Action Time Min (MS)", 110, 1, 500, 1));
        this.registerSetting(waitMsMax = new SliderSetting("Action Time Max (MS)", 150, 1, 500, 1));
        this.registerSetting(hitPerMin = new SliderSetting("Once every Min hits", 1, 1, 10, 1));
        this.registerSetting(hitPerMax = new SliderSetting("Once every Max hits", 1, 1, 10, 1));
        this.registerSetting(postDelayMin = new SliderSetting("Post Delay Min (MS)", 10, 0, 500, 1));
        this.registerSetting(postDelayMax = new SliderSetting("Post Delay Max (MS)", 40, 0, 500, 1));
        this.registerSetting(chance = new SliderSetting("Chance %", 100, 0, 100, 1));
        this.registerSetting(range = new SliderSetting("Range", 3, 1, 6, 0.05));
        this.registerSetting(eventType = new ModeSetting("Event Type", new String[]{"PRE", "POST"}, 1));
    }

    private void finishCombo() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        Utils.setMouseButtonState(1, false);
    }

    private void startCombo() {
        if (!Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) return;
        
        int key = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(key, true);
        KeyBinding.onTick(key);
        Utils.setMouseButtonState(1, true);
    }

    @Override
    public void guiUpdate() {
        Utils.correctValue(waitMsMin, waitMsMax);
        Utils.correctValue(hitPerMin, hitPerMax);
        Utils.correctValue(postDelayMin, postDelayMax);
    }

    @SubscribeEvent
    public void onTick(TickEvent.RenderTickEvent e) {
        if (!Utils.nullCheck()) return;
        if (!Utils.holdingWeapon()) {
            if (executingAction) finishCombo();
            return;
        }

        // Handle right mouse button hold logic
        if (onRightMBHold.isToggled() && !Utils.tryingToCombo()) {
            handleSafeGuard();
            return;
        }

        // Handle post delay timer
        if (waitingForPostDelay) {
            if (postDelayTimer.hasFinished()) {
                startAction();
            }
            return;
        }

        // Handle active action
        if (executingAction) {
            if (actionTimer.hasFinished()) {
                executingAction = false;
                finishCombo();
            }
            return;
        }

        // Check target validity
        Entity target = getValidTarget();
        if (target == null) {
            handleSafeGuard();
            alreadyHit = false;
            return;
        }

        // Check range
        if (mc.thePlayer.getDistanceToEntity(target) > range.getInput()) {
            alreadyHit = false;
            return;
        }

        // Check hurt time based on event type
        boolean isPost = eventType.getInput() == 1;
        if ((isPost && target.hurtResistantTime < 10) || (!isPost && target.hurtResistantTime > 10)) {
            alreadyHit = false;
            return;
        }

        // Check player only
        if (onlyPlayers.isToggled() && !(target instanceof EntityPlayer)) return;
        
        // Check antibot
        if (AntiBot.isBot(target)) return;

        // Handle hit cooldown
        if (hitCoolDown) {
            if (!alreadyHit) {
                hitsWaited++;
                if (hitsWaited >= hitTimeout) {
                    hitCoolDown = false;
                    hitsWaited = 0;
                } else {
                    alreadyHit = true;
                    return;
                }
            } else {
                return;
            }
        }

        if (alreadyHit) return;

        // Check chance
        if (Math.random() * 100 > chance.getInput()) {
            alreadyHit = true;
            return;
        }

        // Start combo
        guiUpdate();
        hitTimeout = (int) Utils.randomValue(hitPerMin.getInput(), hitPerMax.getInput());
        hitCoolDown = true;
        hitsWaited = 0;

        long actionTime = (long) ThreadLocalRandom.current().nextDouble(waitMsMin.getInput(), waitMsMax.getInput() + 0.01);
        actionTimer.setCooldown(actionTime);

        long postDelay = (long) ThreadLocalRandom.current().nextDouble(postDelayMin.getInput(), postDelayMax.getInput() + 0.01);
        
        if (postDelay > 0) {
            postDelayTimer.setCooldown(postDelay);
            postDelayTimer.start();
            waitingForPostDelay = true;
        } else {
            startAction();
        }
        
        alreadyHit = true;
    }

    private void startAction() {
        executingAction = true;
        startCombo();
        actionTimer.start();
        safeGuard = false;
        waitingForPostDelay = false;
    }

    private void handleSafeGuard() {
        if (safeGuard) return;
        safeGuard = true;
        finishCombo();
    }

    private Entity getValidTarget() {
        if (mc.objectMouseOver == null || mc.objectMouseOver.entityHit == null) return null;
        Entity target = mc.objectMouseOver.entityHit;
        if (target.isDead) return null;
        return target;
    }

    @Override
    public void onDisable() {
        finishCombo();
        executingAction = false;
        waitingForPostDelay = false;
        hitCoolDown = false;
        alreadyHit = false;
        safeGuard = false;
        hitsWaited = 0;
    }
}
