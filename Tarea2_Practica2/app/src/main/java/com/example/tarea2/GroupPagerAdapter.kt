package com.example.tarea2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupPagerAdapter(
    private val items: List<GroupInfo>,
    private val onColorReady: (position: Int, color: Int) -> Unit,
    private val onBackImageTap: (group: GroupInfo) -> Unit
) : RecyclerView.Adapter<GroupPagerAdapter.VH>() {

    // Posiciones actualmente volteadas (mostrando reverso)
    private val flippedPositions = mutableSetOf<Int>()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val root: View = v.findViewById(R.id.cardRoot)
        val front: View = v.findViewById(R.id.frontCard)
        val back: View = v.findViewById(R.id.backCard)

        val imgLogo: ImageView = v.findViewById(R.id.imgLogo)   // frente
        val imgPhoto: ImageView = v.findViewById(R.id.imgPhoto) // reverso

        val txtName: TextView = v.findViewById(R.id.txtName)
        val txtSlogan: TextView = v.findViewById(R.id.txtSlogan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
        val vh = VH(v)

        // Anti-sombra por si algún OEM ignora XML
        (vh.front as? com.google.android.material.card.MaterialCardView)?.apply {
            cardElevation = 0f
            stateListAnimator = null
        }
        (vh.back as? com.google.android.material.card.MaterialCardView)?.apply {
            cardElevation = 0f
            stateListAnimator = null
        }
        return vh
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        // Frente: LOGO centrado
        holder.imgLogo.setImageResource(item.logoResId)
        // Reverso: foto full-bleed
        holder.imgPhoto.setImageResource(item.photoResId)

        holder.txtName.text = item.name
        holder.txtSlogan.text = item.slogan

        // Estado inicial según si está volteada esta posición
        val isFlipped = flippedPositions.contains(position)
        applyState(holder, isFlipped, applyInstant = true)

        // Pasar color de fondo sugerido para esta página
        onColorReady(position, item.backgroundColor)

        // Tap frente -> voltear a reverso
        holder.front.setOnClickListener {
            if (!flippedPositions.contains(position)) {
                flipToBack(holder)
                flippedPositions.add(position)
            }
        }

        // Tap foto (reverso) -> navegar a detalle del grupo
        holder.imgPhoto.setOnClickListener {
            onBackImageTap(item)
        }

        // Long-press en foto (reverso) -> volver a frente (sin navegar)
        holder.imgPhoto.setOnLongClickListener {
            if (flippedPositions.contains(position)) {
                flipToFront(holder)
                flippedPositions.remove(position)
            }
            true
        }
    }

    override fun getItemCount(): Int = items.size

    // -------- Animación de flip en dos etapas (90° + 90°) --------

    private fun flipToBack(holder: VH) {
        val duration = 160L
        ensureCamera(holder)
        setClickable(holder, false)

        // 1) Frente: 0° -> 90°
        holder.front.animate()
            .rotationY(90f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                holder.front.visibility = View.GONE
                holder.back.visibility = View.VISIBLE
                holder.back.rotationY = -90f

                // 2) Reverso: -90° -> 0°
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
        ensureCamera(holder)
        setClickable(holder, false)

        // 1) Reverso: 0° -> -90°
        holder.back.animate()
            .rotationY(-90f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                holder.back.visibility = View.GONE
                holder.front.visibility = View.VISIBLE
                holder.front.rotationY = 90f

                // 2) Frente: 90° -> 0°
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
        holder.imgPhoto.isClickable = clickable
        holder.imgLogo.isClickable = clickable
    }

    private fun applyState(holder: VH, showBack: Boolean, applyInstant: Boolean) {
        if (showBack) {
            holder.front.visibility = View.GONE
            holder.back.visibility = View.VISIBLE
            if (applyInstant) {
                holder.front.rotationY = 180f
                holder.back.rotationY = 0f
            }
        } else {
            holder.front.visibility = View.VISIBLE
            holder.back.visibility = View.GONE
            if (applyInstant) {
                holder.front.rotationY = 0f
                holder.back.rotationY = -180f
            }
        }
    }
}
