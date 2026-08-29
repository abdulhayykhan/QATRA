import 'package:flutter/material.dart';

class QatraTheme {
  // QATRA Brand Palette
  static const Color qatraRedPrimary = Color(0xFFD32F2F);
  static const Color qatraRedDark = Color(0xFFB71C1C);
  static const Color qatraRedContainer = Color(0xFFFFEBEE);
  static const Color qatraRedContainerDark = Color(0xFFFFCDD2);
  static const Color qatraRedSurface = Color(0xFFFFF5F5);

  static const Color qatraWhite = Color(0xFFFFFFFF);
  static const Color qatraOffWhite = Color(0xFFFBFBFC);
  static const Color qatraGray200 = Color(0xFFE9ECEF);
  static const Color qatraGray400 = Color(0xFFCED4DA);
  static const Color qatraGray800 = Color(0xFF343A40);
  static const Color qatraGray900 = Color(0xFF212529);

  static const Color qatraWarning = Color(0xFFF57C00);

  // Dark Palette
  static const Color qatraDarkBackground = Color(0xFF121212);
  static const Color qatraDarkSurface = Color(0xFF1E1E1E);
  static const Color qatraDarkSurfaceVariant = Color(0xFF2C2C2C);
  static const Color qatraDarkPrimary = Color(0xFFFF8A80);
  static const Color qatraDarkPrimaryContainer = Color(0xFF5F1414);

  static ThemeData get lightTheme {
    return ThemeData(
      useMaterial3: true,
      colorScheme: const ColorScheme.light(
        primary: qatraRedPrimary,
        onPrimary: qatraWhite,
        primaryContainer: qatraRedContainer,
        onPrimaryContainer: qatraRedDark,
        secondary: qatraRedDark,
        onSecondary: qatraWhite,
        secondaryContainer: qatraRedContainerDark,
        onSecondaryContainer: qatraRedDark,
        tertiary: qatraWarning,
        background: qatraOffWhite,
        onBackground: qatraGray900,
        surface: qatraWhite,
        onSurface: qatraGray900,
        surfaceVariant: qatraRedSurface,
        onSurfaceVariant: qatraGray800,
        outline: qatraGray400,
        outlineVariant: qatraGray200,
      ),
    );
  }

  static ThemeData get darkTheme {
    return ThemeData(
      useMaterial3: true,
      colorScheme: const ColorScheme.dark(
        primary: qatraDarkPrimary,
        onPrimary: Colors.black,
        primaryContainer: qatraDarkPrimaryContainer,
        onPrimaryContainer: Color(0xFFFFDAD6),
        secondary: Color(0xFFFFB4AB),
        onSecondary: Color(0xFF690005),
        secondaryContainer: Color(0xFF93000A),
        onSecondaryContainer: Color(0xFFFFDAD6),
        tertiary: Color(0xFFFFB77C),
        background: qatraDarkBackground,
        onBackground: Color(0xFFEDE0DF),
        surface: qatraDarkSurface,
        onSurface: Color(0xFFEDE0DF),
        surfaceVariant: qatraDarkSurfaceVariant,
        onSurfaceVariant: Color(0xFFD8C2BF),
        outline: Color(0xFFA08C8A),
      ),
    );
  }
}
