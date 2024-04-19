package me.study.util.models

import androidx.compose.ui.graphics.Color
import me.study.ui.theme.AngryColor
import me.study.ui.theme.AwfulColor
import me.study.ui.theme.BoredColor
import me.study.ui.theme.CalmColor
import me.study.ui.theme.DepressedColor
import me.study.ui.theme.DisappointedColor
import me.study.ui.theme.HappyColor
import me.study.ui.theme.HumorousColor
import me.study.ui.theme.LonelyColor
import me.study.ui.theme.MysteriousColor
import me.study.ui.theme.NeutralColor
import me.study.ui.theme.RomanticColor
import me.study.ui.theme.ShamefulColor
import me.study.ui.theme.SurprisedColor
import me.study.ui.theme.SuspiciousColor
import me.study.ui.theme.TenseColor
import me.study.util.R

enum class Mood(
    val icon: Int,
    val containerColor: Color,
    val contentColor: Color
) {
    Neutral(
        icon = R.drawable.neutral,
        contentColor = Color.Black,
        containerColor = NeutralColor
    ),
    Happy(
        icon = R.drawable.happy,
        contentColor = Color.Black,
        containerColor = HappyColor
    ),
    Angry(
        icon = R.drawable.angry,
        contentColor = Color.White,
        containerColor = AngryColor
    ),
    Bored(
        icon = R.drawable.bored,
        contentColor = Color.Black,
        containerColor = BoredColor
    ),
    Calm(
        icon = R.drawable.calm,
        contentColor = Color.Black,
        containerColor = CalmColor
    ),
    Depressed(
        icon = R.drawable.depressed,
        contentColor = Color.Black,
        containerColor = DepressedColor
    ),
    Disappointed(
        icon = R.drawable.disappointed,
        contentColor = Color.White,
        containerColor = DisappointedColor
    ),
    Humorous(
        icon = R.drawable.humorous,
        contentColor = Color.Black,
        containerColor = HumorousColor
    ),
    Lonely(
        icon = R.drawable.lonely,
        contentColor = Color.White,
        containerColor = LonelyColor
    ),
    Mysterious(
        icon = R.drawable.mysterious,
        contentColor = Color.Black,
        containerColor = MysteriousColor
    ),
    Romantic(
        icon = R.drawable.romantic,
        contentColor = Color.White,
        containerColor = RomanticColor
    ),
    Shameful(
        icon = R.drawable.shameful,
        contentColor = Color.White,
        containerColor = ShamefulColor
    ),
    Awful(
        icon = R.drawable.awful,
        contentColor = Color.Black,
        containerColor = AwfulColor
    ),
    Surprised(
        icon = R.drawable.surprised,
        contentColor = Color.Black,
        containerColor = SurprisedColor
    ),
    Suspicious(
        icon = R.drawable.suspicious,
        contentColor = Color.Black,
        containerColor = SuspiciousColor
    ),
    Tense(
        icon = R.drawable.tense,
        contentColor = Color.Black,
        containerColor = TenseColor
    )
}