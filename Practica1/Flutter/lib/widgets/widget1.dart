import 'package:flutter/material.dart';

class Widget1 extends StatelessWidget {
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
            const Text("📝 TextFields",
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            const Text("Sirven para que el usuario escriba información."),
            const SizedBox(height: 12),
            const TextField(
              decoration: InputDecoration(
                border: OutlineInputBorder(),
                labelText: "Escribe algo...",
              ),
            ),
          ],
        ),
      ),
    );
  }
}
