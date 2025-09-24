import 'package:flutter/material.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'screens/second_screen.dart';
import 'widgets/widget1.dart';
import 'widgets/widget2.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Practica 1 Flutter',
      theme: ThemeData(
        primarySwatch: Colors.purple,
      ),
      home: MainScreen(),
    );
  }
}

class MainScreen extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFE6E6FA), // lavanda claro
      appBar: AppBar(
        backgroundColor: const Color(0xFFD8BFD8), // lavanda acento
        title: const Text("Activity 1"),
        actions: [
          IconButton(
            icon: const Icon(Icons.arrow_forward),
            onPressed: () {
              Fluttertoast.showToast(msg: "Navegando a Activity 2");
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => SecondScreen()),
              );
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            Widget1(),
            const SizedBox(height: 20),
            Widget2(),
          ],
        ),
      ),
    );
  }
}
