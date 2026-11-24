package yuxihe.extensiontest.skill;

/**
 * 第二技能的全局常量。客户端按键注入与服务端解析必须使用同一个位。
 */
public final class SecondSkillConstants {

    /** keys 位掩码中第二技能占用的位：第 9 位。SBW 已用第 0~8 位。 */
    public static final short SECOND_SKILL_BIT = 512;

    /** 技能冷却（tick）。20 tick = 1 秒。 */
    public static final int COOLDOWN_TICKS = 200;

    /** 修理持续事件 */
    public static final int REPAIR_DURATION = 100;

    /** 修理技能回复量。 */
    public static final float REPAIR_AMOUNT = 1.0F;
    public static final float FIRST_REPAIR_AMOUNT = 10.0F;

    /** 载具 NBT 中决定技能类型的键：repair / defense。缺省为 repair。 */
    public static final String NBT_SKILL_TYPE = "SecondSkillType";

    public static final String SKILL_REPAIR = "repair";
    public static final String SKILL_DEFENSE = "defense";

    public static final float COMPONENT_MAX_HEALTH = 500.0F;


    private SecondSkillConstants() {}
}
