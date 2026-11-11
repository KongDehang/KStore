package com.kk.detain.domain

import java.time.LocalDate
import kotlin.random.Random

object Encouragements {
    val lines: List<String> = listOf(
        "你已经撑过了这一秒，这就够了。",
        "想打开它的那一刻，正是它最想偷走你的时候。",
        "放下手机的手，比拿起手机的手更有力量。",
        "不是所有想念，都需要马上回应。",
        "你不需要它，你只是有点无聊。",
        "这一分钟留给呼吸，下一分钟留给生活。",
        "看，你正在成为自己的主人。",
        "忍住一次，就多认识自己一点。",
        "时间不是敌人，滑动才是。",
        "真正的自由，是随时都可以离开。",
        "叶子不会因为风的邀请就离开树枝。",
        "把注意力当成钱，你会随便花吗？",
        "现在的空虚，不是刷屏能填满的。",
        "你在等的，不会在那块屏幕里出现。",
        "慢一点，世界不会错过你。",
        "一次克制，胜过十次决心。",
        "锁住的不是应用，是你想去的地方。",
        "今天少刷十分钟，明天多记得一点自己。",
        "你比算法更懂你想要什么。",
        "停下来的那一刻，时间才属于你。",
        "把手机放下，把手抬起来，抱抱身边的人。",
        "心痒会过去，就像所有情绪一样。",
        "别用别人的生活，填自己的空档。",
        "无聊是灵感的入口，不是刷屏的理由。",
        "你已经走了这么远，不要在这里停下。",
        "深呼吸。你不需要立刻做任何事。",
    )

    fun random(excluding: String? = null): String {
        val pool = if (excluding == null) lines else lines.filterNot { it == excluding }
        return pool[Random.nextInt(pool.size)]
    }
}

data class Tip(val title: String, val body: String)

object Tips {
    val all: List<Tip> = listOf(
        Tip("给应用一个偏远的位置", "把最想减少的应用移出首屏。多一步操作，就多一次犹豫。"),
        Tip("无聊时先做一件小事", "喝口水、站起来走两步、看一眼窗外，等 60 秒再决定要不要打开。"),
        Tip("关掉非必要通知", "红点不是任务，它只是别人的期待。"),
        Tip("给睡前留一道门槛", "把充电线放到卧室外，让睡前半小时只属于你和困意。"),
        Tip("用替代，而不是禁止", "想刷手机时，先读两页书或听一首完整的歌，再决定。"),
        Tip("每周留一段无屏时间", "哪怕只有 30 分钟，散步、发呆、和朋友聊天都可以。"),
        Tip("把屏幕调成灰度", "颜色越淡，刺激越小，手指也会慢慢安静下来。"),
        Tip("记录一次冲动", "心痒时在便签写下「我想打开它，因为…」，常常写一半就不想了。"),
        Tip("给早晨留 20 分钟", "醒来先不看手机，把最清醒的时间留给自己。"),
        Tip("允许自己真正休息", "真正的休息不是换一个屏幕，而是换一种身体状态。"),
        Tip("把它当成一场小实验", "只看今天，不看永远。今天做到了，就是成功。"),
        Tip("让身边的人知道你的计划", "被看见的承诺，更容易坚持。"),
    )

    /** Three stable tips for the day, so the home screen does not reshuffle on every recomposition. */
    fun forToday(date: LocalDate = LocalDate.now(), count: Int = 3): List<Tip> {
        val start = (date.toEpochDay() % all.size).toInt()
        return List(count.coerceAtMost(all.size)) { index -> all[(start + index) % all.size] }
    }
}
