package com.example.tarea2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2

class GroupDetailActivity : AppCompatActivity() {

    private lateinit var imgHeader: ImageView
    private lateinit var tvGroupName: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvDebut: TextView
    private lateinit var tvFandom: TextView
    private lateinit var imgComeback: ImageView
    private lateinit var viewPagerMembers: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeApplier.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_detail)

        imgHeader = findViewById(R.id.imgHeader)
        tvGroupName = findViewById(R.id.tvGroupName)
        tvDescription = findViewById(R.id.tvDescription)
        tvDebut = findViewById(R.id.tvDebut)
        tvFandom = findViewById(R.id.tvFandom)
        imgComeback = findViewById(R.id.imgComeback)
        viewPagerMembers = findViewById(R.id.viewPagerMembers)

        val groupId = intent.getStringExtra("group_id") ?: "twice"
        val detail = buildDetail(groupId)

        // Imagen principal [group]_photo_2
        imgHeader.setImageResource(detail.headerPhotoResId)
        // Info
        tvGroupName.text = detail.name
        tvDescription.text = detail.description
        tvDebut.text = "Fecha de debut: ${detail.debut}"
        tvFandom.text = "Fandom: ${detail.fandom}"

        // Último comeback -> click abre Spotify
        imgComeback.setImageResource(detail.comebackCoverResId)
        imgComeback.setOnClickListener {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(detail.spotifyUrl))
            startActivity(i)
        }

        // Carrusel de miembros (con color y logo del grupo para el reverso)
        val adapter = MembersPagerAdapter(
            items = detail.members,
            groupColor = detail.groupColor,
            groupLogoResId = detail.groupLogoResId
        )
        viewPagerMembers.adapter = adapter
        viewPagerMembers.offscreenPageLimit = 1
        viewPagerMembers.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        viewPagerMembers.setPageTransformer(MarginPageTransformer(24))
    }

    private fun buildDetail(groupId: String): GroupDetail {
        return when (groupId) {
            "twice" -> GroupDetail(
                name = "TWICE",
                headerPhotoResId = R.drawable.twice_photo_2,
                description = "Grupo femenino de JYP debutado tras Sixteen, conocido por su energía brillante y coreografías pegajosas.",
                debut = "20 Oct 2015",
                fandom = "ONCE",
                comebackCoverResId = R.drawable.twice_comeback_cover,
                spotifyUrl = "https://open.spotify.com/intl-es/album/02dUOPB3MtmV5fawzaZiq6?si=cqnx4ysxQ9S-XMAQhGo1WA",
                groupColor = android.graphics.Color.parseColor("#fc5d9d"),
                groupLogoResId = R.drawable.twice_logo,
                members = listOf(
                    MemberInfo("Nayeon", "22 Sep 1995", R.drawable.twice_nayeon),
                    MemberInfo("Jeongyeon", "01 Nov 1996", R.drawable.twice_jeongyeon),
                    MemberInfo("Momo", "09 Nov 1996", R.drawable.twice_momo),
                    MemberInfo("Sana", "29 Dic 1996", R.drawable.twice_sana),
                    MemberInfo("Jihyo", "01 Feb 1997", R.drawable.twice_jihyo),
                    MemberInfo("Mina", "24 Mar 1997", R.drawable.twice_mina),
                    MemberInfo("Dahyun", "28 May 1998", R.drawable.twice_dahyun),
                    MemberInfo("Chaeyoung", "23 Abr 1999", R.drawable.twice_chaeyoung),
                    MemberInfo("Tzuyu", "14 Jun 1999", R.drawable.twice_tzuyu)
                )
            )
            "straykids" -> GroupDetail(
                name = "Stray Kids",
                headerPhotoResId = R.drawable.straykids_photo_2,
                description = "Boy group con fuerte autoría musical, rap poderoso y presentaciones intensas.",
                debut = "25 Mar 2018",
                fandom = "STAY",
                comebackCoverResId = R.drawable.straykids_comeback_cover,
                spotifyUrl = "https://open.spotify.com/intl-es/album/3wqskwruUGJHC4yHbo7nxc?si=nq-JkJhkQuOWCoRkYwrgYw",
                groupColor = android.graphics.Color.parseColor("#ab0033"),
                groupLogoResId = R.drawable.straykids_logo,
                members = listOf(
                    MemberInfo("Bang Chan", "03 Oct 1997", R.drawable.skz_bangchan),
                    MemberInfo("Lee Know", "25 Oct 1998", R.drawable.skz_leeknow),
                    MemberInfo("Changbin", "11 Ago 1999", R.drawable.skz_changbin),
                    MemberInfo("Hyunjin", "20 Mar 2000", R.drawable.skz_hyunjin),
                    MemberInfo("Han", "14 Sep 2000", R.drawable.skz_han),
                    MemberInfo("Felix", "15 Sep 2000", R.drawable.skz_felix),
                    MemberInfo("Seungmin", "22 Sep 2000", R.drawable.skz_seungmin),
                    MemberInfo("I.N", "08 Feb 2001", R.drawable.skz_in)
                )
            )
            "itzy" -> GroupDetail(
                name = "ITZY",
                headerPhotoResId = R.drawable.itzy_photo_2,
                description = "Girl group con mensajes de confianza y performances carismáticos.",
                debut = "12 Feb 2019",
                fandom = "MIDZY",
                comebackCoverResId = R.drawable.itzy_comeback_cover,
                spotifyUrl = "https://open.spotify.com/intl-es/album/0bVAPVpPL25nIfko4O1G4J?si=Qm5htnLhSs6YvAsI6fYx8g",
                groupColor = android.graphics.Color.parseColor("#FFB3DE"), // magenta pastel
                groupLogoResId = R.drawable.itzy_logo,
                members = listOf(
                    MemberInfo("Yeji", "26 May 2000", R.drawable.itzy_yeji),
                    MemberInfo("Lia", "21 Jul 2000", R.drawable.itzy_lia),
                    MemberInfo("Ryujin", "17 Apr 2001", R.drawable.itzy_ryujin),
                    MemberInfo("Chaeryeong", "05 Jun 2001", R.drawable.itzy_chaeryeong),
                    MemberInfo("Yuna", "09 Dic 2003", R.drawable.itzy_yuna)
                )
            )
            "nmixx" -> GroupDetail(
                name = "NMIXX",
                headerPhotoResId = R.drawable.nmixx_photo_2,
                description = "Concepto ‘MIXX POP’ con cambios de género dentro de una misma canción.",
                debut = "22 Feb 2022",
                fandom = "NSWER",
                comebackCoverResId = R.drawable.nmixx_comeback_cover,
                spotifyUrl = "https://open.spotify.com/intl-es/album/1qOD7pel3w9en2JKQ3l6Ha?si=z4E16Ob4Rj6swP1Jxo7NLw",
                groupColor = android.graphics.Color.parseColor("#555C66"),
                groupLogoResId = R.drawable.nmixx_logo,
                members = listOf(
                    MemberInfo("Lily", "17 Oct 2002", R.drawable.nmixx_lily),
                    MemberInfo("Haewon", "25 Feb 2003", R.drawable.nmixx_haewon),
                    MemberInfo("Sullyoon", "26 Ene 2004", R.drawable.nmixx_sullyoon),
                    MemberInfo("Bae", "28 Dic 2004", R.drawable.nmixx_bae),
                    MemberInfo("Jiwoo", "13 Abr 2005", R.drawable.nmixx_jiwoo),
                    MemberInfo("Kyujin", "26 May 2003", R.drawable.nmixx_kyujin)
                )
            )
            "xdh" -> GroupDetail(
                name = "Xdinary Heroes",
                headerPhotoResId = R.drawable.xdh_photo_2,
                description = "Banda de JYP bajo Studio J con enfoque rock e instrumentación en vivo.",
                debut = "06 Dec 2021",
                fandom = "Villains",
                comebackCoverResId = R.drawable.xdh_comeback_cover,
                spotifyUrl = "https://open.spotify.com/intl-es/album/0h4U9eKaISbwP93lglW6s6?si=9lYJ4nfTRdyVinSvSVFfIQ",
                groupColor = android.graphics.Color.parseColor("#A7B8C8"),
                groupLogoResId = R.drawable.xdh_logo,
                members = listOf(
                    MemberInfo("Gunil", "24 Jul 1998", R.drawable.xdh_gunil),
                    MemberInfo("Jungsu", "26 Jun 2001", R.drawable.xdh_jungsu),
                    MemberInfo("Gaon", "14 Ene 2002", R.drawable.xdh_gaon),
                    MemberInfo("O.de", "11 Jun 2002", R.drawable.xdh_ode),
                    MemberInfo("Jun Han", "18 Ago 2002", R.drawable.xdh_junhan),
                    MemberInfo("Jooyeon", "12 Sep 2002", R.drawable.xdh_jooyeon)
                )
            )
            else -> GroupDetail(
                name = groupId.uppercase(),
                headerPhotoResId = R.drawable.twice_photo_2,
                description = "Descripción no disponible.",
                debut = "—",
                fandom = "—",
                comebackCoverResId = R.drawable.twice_comeback_cover,
                spotifyUrl = "https://open.spotify.com/",
                groupColor = android.graphics.Color.LTGRAY,
                groupLogoResId = R.drawable.jyp_logo_hero,
                members = emptyList()
            )
        }
    }
}

data class GroupDetail(
    val name: String,
    val headerPhotoResId: Int,
    val description: String,
    val debut: String,
    val fandom: String,
    val comebackCoverResId: Int,
    val spotifyUrl: String,
    val groupColor: Int,
    val groupLogoResId: Int,
    val members: List<MemberInfo>
)
