import 'package:flutter_test/flutter_test.dart';
import 'package:demonitalk_pro/main.dart';

void main() {
  testWidgets('DemoniTalkApp smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const DemoniTalkApp());
    expect(find.textContaining('Demoni'), findsWidgets);
  });
}
