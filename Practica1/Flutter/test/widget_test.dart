import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:practica1_flutter/main.dart';

void main() {
  group('App Principal', () {
    testWidgets('Activity 1 carga correctamente', (WidgetTester tester) async {
      // Construye la app
      await tester.pumpWidget(MyApp());

      // Verifica que aparece el título "Activity 1"
      expect(find.text('Activity 1'), findsOneWidget);

      // Verifica que el botón de navegación a Activity 2 exista
      expect(find.byIcon(Icons.arrow_forward), findsOneWidget);

      // Verifica que los widgets de Activity 1 existan (Widget1 a Widget5)
      expect(find.text('TextFields'), findsOneWidget);
      expect(find.text('Botones'), findsOneWidget);
      expect(find.text('Elementos de selección'), findsOneWidget);
      expect(find.text('Listas'), findsOneWidget);
      expect(find.text('Elementos de información'), findsOneWidget);
    });

    testWidgets('Navegación a Activity 2', (WidgetTester tester) async {
      await tester.pumpWidget(MyApp());

      // Toca el botón de flecha
      await tester.tap(find.byIcon(Icons.arrow_forward));
      await tester.pumpAndSettle();

      // Verifica que se abrió Activity 2
      expect(find.text('Activity 2'), findsOneWidget);

      // Verifica que los widgets de Activity 2 existan
      expect(find.text('TextFields'), findsOneWidget);
      expect(find.text('Botones'), findsOneWidget);
      expect(find.text('Simulación de carga'), findsOneWidget);
    });

    testWidgets('Botones muestran Toast (simulado)', (WidgetTester tester) async {
      await tester.pumpWidget(MyApp());

      // Toca un botón de ejemplo en Activity 1
      final buttonFinder = find.text('Mostrar Toast');
      if (buttonFinder.evaluate().isNotEmpty) {
        await tester.tap(buttonFinder);
        await tester.pump();
      }

      // Como fluttertoast no aparece en el árbol de widgets,
      // solo verificamos que el botón exista y pueda ser presionado
      expect(buttonFinder, findsOneWidget);
    });

    testWidgets('Navegación de Activity 2 de regreso a Activity 1', (WidgetTester tester) async {
      await tester.pumpWidget(MyApp());

      // Navega a Activity 2
      await tester.tap(find.byIcon(Icons.arrow_forward));
      await tester.pumpAndSettle();

      // Toca el botón de regresar
      await tester.tap(find.byIcon(Icons.arrow_back));
      await tester.pumpAndSettle();

      // Verifica que volvió a Activity 1
      expect(find.text('Activity 1'), findsOneWidget);
    });
  });
}
