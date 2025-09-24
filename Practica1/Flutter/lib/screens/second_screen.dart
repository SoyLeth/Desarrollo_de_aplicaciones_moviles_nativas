import 'package:flutter/material.dart';
import '../widgets/widget3.dart';
import '../widgets/widget4.dart';
import '../widgets/widget5.dart';

class SecondScreen extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFFD1DC), // rosa pastel claro
      appBar: AppBar(
        backgroundColor: const Color(0xFFFFB6C1), // rosa pastel acento
        title: const Text("Activity 2"),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            Widget3(),
            const SizedBox(height: 20),
            Widget4(),
            const SizedBox(height: 20),
            Widget5(),
          ],
        ),
      ),
    );
  }
}
