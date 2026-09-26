package darksteel.content;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.mod.Mod;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.world.meta.Env;

public class MobileCoreUnit extends Mod {

    public static UnitType mobileCore;

    @Override
    public void loadContent() {
        mobileCore = new CoreUnitType("移动核心");
        Vars.content.units().add(mobileCore);
        Log.info("[DarkSteel] 移动核心单位加载完成");
    }

    @Override
    public void init() {
        mobileCore.alwaysUnlocked = true;
    }
}

class CoreUnitType extends UnitType {

    public CoreUnitType(String name) {
        super(name);

        health = 5000f;
        armor = 5f;
        hitSize = 32f;

        flying = true;
        lowAltitude = false;
        speed = 1.2f;
        accel = 0.08f;
        drag = 0.015f;
        rotateSpeed = 2f;

        itemCapacity = 3000;

        playerControllable = true;
        useUnitCap = false;
        targetable = true;
        hittable = true;
        logicControllable = true;
        envEnabled = Env.any;

        engineOffset = 12f;
        engineSize = 5f;
        engineColor = Color.valueOf("ffd37f");

        abilities.add(new CoreSupplyAbility(80f, 2f));
    }
}

class CoreSupplyAbility extends Ability {

    public float range;
    public float supplyRate;
    private float accumulator;

    public CoreSupplyAbility(float range, float supplyRate) {
        this.range = range;
        this.supplyRate = supplyRate;
    }

    public CoreSupplyAbility() {
        this(80f, 1f);
    }

    @Override
    public void update(Unit unit) {
        ItemStack stack = unit.stack;
        if (stack == null || stack.item == null || stack.amount <= 0) return;

        accumulator += supplyRate * Time.delta;
        if (accumulator < 1f) return;

        int totalSupply = (int) accumulator;
        accumulator -= totalSupply;

        final int[] remaining = {Math.min(totalSupply, stack.amount)};
        if (remaining[0] <= 0) return;

        Groups.build.each(build -> {
            if (remaining[0] <= 0) return;
            if (build.team != unit.team) return;

            float dist = Mathf.dst(build.x, build.y, unit.x, unit.y);
            if (dist > range) return;

            // ✅ 用 3 参数签名，第三个参数是 Teamc（传 unit）
            int accepted = build.acceptStack(stack.item, remaining[0], unit);
            if (accepted > 0) {
                stack.amount -= accepted;
                remaining[0] -= accepted;
                // ✅ handleStack 也是 3 参数
                build.handleStack(stack.item, accepted, unit);
            }
        });
    }

    @Override
    public String localized() {
        return "范围内自动供货";
    }

    @Override
    public String toString() {
        return "CoreSupplyAbility";
    }
}