package com.example.tarea2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

data class MemberInfo(
    val name: String,
    val birth: String,
    val photoResId: Int
)

class MembersPagerAdapter(
    private val items: List<MemberInfo>,
    private val groupColor: Int,
    private val groupLogoResId: Int
) : RecyclerView.Adapter<MembersPagerAdapter.VH>() {

    // usamos IDs estables para que el estado sobreviva al reciclado correctamente
    private val flippedIds = mutableSetOf<Long>()

    init {
        setHasStableIds(true)
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val root: View = v.findViewById(R.id.cardRootMember)
        val front: MaterialCardView = v.findViewById(R.id.frontCardMember)
        val back: MaterialCardView = v.findViewById(R.id.backCardMember)

        val imgFront: ImageView = v.findViewById(R.id.imgMemberFront)
        val tvName: TextView = v.findViewById(R.id.tvMemberName)
        val tvBirth: TextView = v.findViewById(R.id.tvMemberBirth)
        val imgGroupLogo: ImageView = v.findViewById(R.id.imgGroupLogo)
    }

    override fun getItemId(position: Int): Long {
        // ID estable derivado del contenido (nombre + nacimiento) — cámbialo si tienes un ID real
        return (items[position].name + "|" + items[position].birth).hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        val vh = VH(v)
        // quitar sombras por si OEM ignora XML
        vh.front.cardElevation = 0f; vh.front.stateListAnimator = null
        vh.back.cardElevation = 0f; vh.back.stateListAnimator = null
        // asegúrate de que los hijos no roben los clicks
        vh.imgFront.isClickable = false; vh.imgFront.isFocusable = false
        vh.imgGroupLogo.isClickable = false; vh.imgGroupLogo.isFocusable = false
        return vh
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        // Frente
        holder.imgFront.setImageResource(item.photoResId)

        // Reverso (color + textos + logo)
        holder.back.setCardBackgroundColor(groupColor)
        holder.tvName.text = item.name
        holder.tvBirth.text = item.birth
        holder.imgGroupLogo.setImageResource(groupLogoResId)

        // Estado inicial basado en ID estable
        val id = getItemId(position)
        val isFlipped = flippedIds.contains(id)
        applyState(holder, isFlipped, true)

        // Click en frente → voltear a reverso
        holder.front.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            val curId = getItemId(pos)
            if (!flippedIds.contains(curId)) {
                flipToBack(holder)
                flippedIds.add(curId)
            }
        }

        // Click en reverso → volver a frente
        holder.back.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            val curId = getItemId(pos)
            if (flippedIds.contains(curId)) {
                flipToFront(holder)
                flippedIds.remove(curId)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // -------- Animación flip (90° + 90°) --------
    private fun flipToBack(holder: VH) {
        val duration = 160L
        ensureCamera(holder); setClickable(holder, false)

        holder.front.animate()
            .rotationY(90f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                holder.front.visibility = View.GONE
                holder.back.visibility = View.VISIBLE
                holder.back.rotationY = -90f

                holder.back.animate()
                    .rotationY(0f)
                    .setDuration(duration)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction { setClickable(holder, true) }
                    .start()
            }
            .start()
    }

    private fun flipToFront(holder: VH) {
        val duration = 160L
        ensureCamera(holder); setClickable(holder, false)

        holder.back.animate()
            .rotationY(-90f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                holder.back.visibility = View.GONE
                holder.front.visibility = View.VISIBLE
                holder.front.rotationY = 90f

                holder.front.animate()
                    .rotationY(0f)
                    .setDuration(duration)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction { setClickable(holder, true) }
                    .start()
            }
            .start()
    }

    private fun ensureCamera(holder: VH) {
        val d = holder.root.resources.displayMetrics.density
        val dist = 8000f * d
        holder.root.cameraDistance = dist
        holder.front.cameraDistance = dist
        holder.back.cameraDistance = dist
    }

    private fun setClickable(holder: VH, clickable: Boolean) {
        holder.front.isClickable = clickable
        holder.back.isClickable = clickable
        // hijos ya quedaron no-clicables en onCreateViewHolder
    }

    private fun applyState(holder: VH, showBack: Boolean, instant: Boolean) {
        if (showBack) {
            holder.front.visibility = View.GONE
            holder.back.visibility = View.VISIBLE
            if (instant) { holder.front.rotationY = 180f; holder.back.rotationY = 0f }
        } else {
            holder.front.visibility = View.VISIBLE
            holder.back.visibility = View.GONE
            if (instant) { holder.front.rotationY = 0f; holder.back.rotationY = -180f }
        }
    }
}
