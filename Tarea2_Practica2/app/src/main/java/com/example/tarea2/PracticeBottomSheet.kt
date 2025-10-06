package com.example.tarea2

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import android.widget.VideoView

class PracticeBottomSheet : BaseScrollableBottomSheet() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.bottomsheet_practice, container, false)

        val video = v.findViewById<VideoView>(R.id.videoPractice)
        val resId = resources.getIdentifier("practice_demo", "raw", requireContext().packageName)
        if (resId != 0) {
            val uri = Uri.parse("android.resource://${requireContext().packageName}/$resId")
            video.setVideoURI(uri)
            video.setOnClickListener {
                if (video.isPlaying) video.pause() else video.start()
            }
        } else {
            Toast.makeText(requireContext(), "Video de práctica no encontrado", Toast.LENGTH_SHORT).show()
            video.visibility = View.GONE
        }
        return v
    }
}
