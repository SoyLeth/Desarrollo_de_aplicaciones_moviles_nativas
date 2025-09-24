import 'package:flutter/material.dart';

class Widget5 extends StatefulWidget {
  @override
  _Widget5State createState() => _Widget5State();
}

class _Widget5State extends State<Widget5> {
  double progress = 0.0;

  void _simulateProgress() {
    setState(() {
      progress = 0.0;
    });

    Future.delayed(const Duration(milliseconds: 300), () {
      for (int i = 1; i <= 10; i++) {
        Future.delayed(Duration(milliseconds: i * 300), () {
          setState(() {
            progress = i / 10;
          });
        });
      }
    });
  }

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
            const Text("ℹ️ Información",
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            const Text("Muestran datos y progreso al usuario."),
            const SizedBox(height: 12),
            const Text("Este es un ejemplo de texto."),
            const SizedBox(height: 12),
            Image.network(
              "https://static.wikia.nocookie.net/hollowknight/images/f/fe/Bench.jpg/revision/latest?cb=20180220144739&path-prefix=es",
              height: 100,
            ),
            const SizedBox(height: 12),
            LinearProgressIndicator(value: progress),
            const SizedBox(height: 8),
            ElevatedButton(
              onPressed: _simulateProgress,
              child: const Text("Simular carga"),
            ),
          ],
        ),
      ),
    );
  }
}
