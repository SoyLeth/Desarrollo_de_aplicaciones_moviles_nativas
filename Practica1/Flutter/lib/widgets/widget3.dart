import 'package:flutter/material.dart';

class Widget3 extends StatefulWidget {
  @override
  _Widget3State createState() => _Widget3State();
}

class _Widget3State extends State<Widget3> {
  bool isChecked = false;
  String radioValue = "Opción 1";
  bool isSwitched = false;

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
            const Text("✅ Selección",
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            const Text("Sirven para seleccionar opciones."),
            const SizedBox(height: 12),
            CheckboxListTile(
              title: const Text("CheckBox"),
              value: isChecked,
              onChanged: (val) {
                setState(() {
                  isChecked = val ?? false;
                });
              },
            ),
            RadioListTile(
              title: const Text("Opción 1"),
              value: "Opción 1",
              groupValue: radioValue,
              onChanged: (val) {
                setState(() {
                  radioValue = val.toString();
                });
              },
            ),
            RadioListTile(
              title: const Text("Opción 2"),
              value: "Opción 2",
              groupValue: radioValue,
              onChanged: (val) {
                setState(() {
                  radioValue = val.toString();
                });
              },
            ),
            SwitchListTile(
              title: const Text("Interruptor"),
              value: isSwitched,
              onChanged: (val) {
                setState(() {
                  isSwitched = val;
                });
              },
            ),
          ],
        ),
      ),
    );
  }
}
