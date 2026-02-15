package keystrokesmod.module.impl.combat.morekb;

import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class SimpleSprintReset extends SubMode<IMoreKB> {
    private final SliderSetting minRePressDelay;
    private final SliderSetting maxRePressDelay;
    private final SliderSetting minDelayBetween;
    private final SliderSetting maxDelayBetween;
    private final SliderSetting chance;
    private final ButtonSetting playersOnly;
    private final ButtonSetting notWhileRunner;
    private final ButtonSetting wTapCompatibility;

    private int delayTicksLeft = 0;
    private int reSprintTicksLeft = -1;
    private final Random random = new Random();
    private boolean isSTapMode = false;

    public SimpleSprintReset(String name, @NotNull IMoreKB parent) {
        super(name, parent);
        this.isSTapMode = name.toLowerCase().contains("stap") || name.toLowerCase().contains("s-tap");
        
        this.registerSetting(minRePressDelay = new SliderSetting("Min Re-press delay", 2, 0, 10, 1, "ticks"));
        this.registerSetting(maxRePressDelay = new SliderSetting("Max Re-press delay", 4, 0, 10, 1, "ticks"));
        this.registerSetting(minDelayBetween = new SliderSetting("Min delay between", 10, 0, 13, 1, "ticks"));
        this.registerSetting(maxDelayBetween = new SliderSetting("Max delay between", 10, 0, 13, 1, "ticks"));
        this.registerSetting(chance = new SliderSetting("Chance", 100, 0, 100, 1, "%"));
        this.registerSetting(playersOnly = new ButtonSetting("Players only", true));
        this.registerSetting(notWhileRunner = new ButtonSetting("Not while runner", false));
        
        // Only show for STap mode
        if (isSTapMode) {
            this.registerSetting(wTapCompatibility = new ButtonSetting("WTap compatibility", true));
        } else {
            wTapCompatibility = null;
        }
    }

    @Override
    public void guiUpdate() throws Exception {
        Utils.correctValue(minRePressDelay, maxRePressDelay);
        Utils.correctValue(minDelayBetween, maxDelayBetween);
    }

    @Override
    public void onUpdate() throws Exception {
        if (reSprintTicksLeft == 0) {
            parent.reSprint();
            reSprintTicksLeft = -1;
        } else if (reSprintTicksLeft > 0) {
            reSprintTicksLeft--;
        }

        if (delayTicksLeft > 0)
            delayTicksLeft--;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttack(AttackEntityEvent event) {
        if (!Utils.nullCheck() || event.entityPlayer != mc.thePlayer || delayTicksLeft > 0) return;
        if (!(event.target instanceof EntityLivingBase)) return;
        if (playersOnly.isToggled() && !(event.target instanceof EntityPlayer)) return;
        if (notWhileRunner.isToggled() && !Utils.inFov(180, event.target, mc.thePlayer)) return;
        if (((EntityLivingBase) event.target).deathTime != 0) return;
        if (AntiBot.isBot(event.target)) return;

        if (random.nextDouble() * 100 > chance.getInput()) return;

        // STap with WTap compatibility: Don't conflict if WTap is active
        if (isSTapMode && wTapCompatibility != null && wTapCompatibility.isToggled()) {
            // Check if external WTap module is active (via reflection or shared state)
            // For now, we assume WTap takes priority if enabled
            if (isExternalWTapActive()) {
                return; // Let WTap handle this hit
            }
        }

        parent.stopSprint();

        reSprintTicksLeft = Utils.randomizeInt((int)minRePressDelay.getInput(), (int)maxRePressDelay.getInput());
        delayTicksLeft = reSprintTicksLeft + Utils.randomizeInt((int)minDelayBetween.getInput(), (int)maxDelayBetween.getInput());
    }
    
    /**
     * Check if external WTap module is currently active
     * This prevents double sprint reset conflicts
     */
    private boolean isExternalWTapActive() {
        try {
            // Check if WTap module exists and is resetting sprint
            keystrokesmod.module.ModuleManager moduleManager = keystrokesmod.module.ModuleManager.getInstance();
            if (moduleManager != null) {
                Object wtap = moduleManager.getModule("WTap");
                if (wtap != null) {
                    // Check if enabled and active via reflection
                    java.lang.reflect.Method isEnabled = wtap.getClass().getMethod("isEnabled");
                    if ((boolean)isEnabled.invoke(wtap)) {
                        // Check if currently resetting
                        java.lang.reflect.Field activeField = wtap.getClass().getDeclaredField("active");
                        activeField.setAccessible(true);
                        return (boolean)activeField.get(wtap);
                    }
                }
            }
        } catch (Exception e) {
            // If reflection fails, assume no conflict
            return false;
        }
        return false;
    }
}
