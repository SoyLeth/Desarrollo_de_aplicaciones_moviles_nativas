package com.example.tarea2

import android.app.Dialog
import android.os.Bundle
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Aplica:
 * - arrastre (isDraggable = true)
 * - altura inicial (peekHeight ~40% pantalla)
 * - scroll interno (usa NestedScrollView en el layout)
 * - cerrar al deslizar hacia abajo (isHideable = true + dismissWithAnimation)
 */
open class BaseScrollableBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            setDismissWithAnimation(true)
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet =
            dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return
        val behavior = BottomSheetBehavior.from(bottomSheet)

        // Altura inicial (colapsado) ~40% de la pantalla
        val peek = (resources.displayMetrics.heightPixels * 0.40f).toInt()

        behavior.apply {
            isFitToContents = true
            isDraggable = true
            isHideable = true         // permite deslizar para cerrar
            skipCollapsed = false
            peekHeight = peek
            state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }
}
