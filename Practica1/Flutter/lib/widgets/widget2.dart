import 'package:flutter/material.dart';
import 'package:fluttertoast/fluttertoast.dart';

class Widget2 extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 4,
      margin: const EdgeInsets.all(8),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text("🔘 Botones",
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            const Text("Permiten ejecutar acciones al hacer clic."),
            const SizedBox(height: 12),
            ElevatedButton(
              onPressed: () {
                Fluttertoast.showToast(msg: "Botón presionado");
              },
              child: const Text("Presionar"),
            ),
            const SizedBox(height: 12),
            IconButton(
              icon: const Icon(Icons.favorite, color: Colors.pink),
              onPressed: () {
                Fluttertoast.showToast(msg: "IconButton presionado");
              },
            ),
          ],
        ),
      ),
    );
  }
}
