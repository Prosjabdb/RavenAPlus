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

public class SimpleSprintReset extends SubMode<IMoreKB> {
    private final SliderSetting minRePressDelay;
    private final SliderSetting maxRePressDelay;
    private final SliderSetting minDelayBetween;
    private final SliderSetting maxDelayBetween;
    private final SliderSetting chance;
    private final ButtonSetting playersOnly;
    private final ButtonSetting notWhileRunner;

    private int delayTicksLeft = 0;
    private int reSprintTicksLeft = -1;
    private boolean wasSprinting = false;

    public SimpleSprintReset(String name, @NotNull IMoreKB parent) {
        super(name, parent);
        this.registerSetting(minRePressDelay = new SliderSetting("Min Re-press delay", 2, 0, 10, 1));
        this.registerSetting(maxRePressDelay = new SliderSetting("Max Re-press delay", 4, 0, 10, 1));
        this.registerSetting(minDelayBetween = new SliderSetting("Min delay between", 10, 0, 13, 1));
        this.registerSetting(maxDelayBetween = new SliderSetting("Max delay between", 10, 0, 13, 1));
        this.registerSetting(chance = new SliderSetting("Chance", 100, 0, 100, 1));
        this.registerSetting(playersOnly = new ButtonSetting("Players only", true));
        this.registerSetting(notWhileRunner = new ButtonSetting("Not while runner", false));
    }

    @Override
    public void guiUpdate() throws Exception {
        Utils.correctValue(minRePressDelay, maxRePressDelay);
        Utils.correctValue(minDelayBetween, maxDelayBetween);
    }

    @Override
    public void onUpdate() throws Exception {
        // Handle re-sprint timing
        if (reSprintTicksLeft == 0) {
            if (wasSprinting) {
                parent.reSprint();
            }
            reSprintTicksLeft = -1;
            wasSprinting = false;
        } else if (reSprintTicksLeft > 0) {
            reSprintTicksLeft--;
        }

        // Decrement delay
        if (delayTicksLeft > 0) {
            delayTicksLeft--;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttack(AttackEntityEvent event) {
        if (!Utils.nullCheck()) return;
        if (event.entityPlayer != mc.thePlayer) return;
        if (delayTicksLeft > 0) return;
        
        if (!(event.target instanceof EntityLivingBase)) return;
        EntityLivingBase target = (EntityLivingBase) event.target;
        
        if (playersOnly.isToggled() && !(target instanceof EntityPlayer)) return;
        if (notWhileRunner.isToggled() && !Utils.inFov(180, target, mc.thePlayer)) return;
        if (target.deathTime != 0) return;
        if (AntiBot.isBot(target)) return;
        if (Math.random() * 100 > chance.getInput()) return;

        // Store sprint state before stopping
        wasSprinting = mc.thePlayer.isSprinting();
        
        parent.stopSprint();

        reSprintTicksLeft = Utils.randomizeInt((int) minRePressDelay.getInput(), (int) maxRePressDelay.getInput());
        int betweenDelay = Utils.randomizeInt((int) minDelayBetween.getInput(), (int) maxDelayBetween.getInput());
        delayTicksLeft = reSprintTicksLeft + betweenDelay;
    }
}
