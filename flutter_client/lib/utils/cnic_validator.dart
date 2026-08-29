class CnicValidator {
  static const List<List<int>> _validProvinceDistrictRanges = [
    [1, 16],
    [21, 29],
    [31, 39],
    [41, 49],
    [51, 59],
    [61, 69],
    [71, 79],
    [81, 89]
  ];

  /// Format and district-code validation only. Pakistani CNICs have no checksum
  /// digit - this does NOT confirm the CNIC is real or belongs to the submitting
  /// user. Real identity confirmation requires NADRA Verisys integration.
  static bool validatePakistaniCnic(String cnicNumber) {
    final normalized = cnicNumber.trim().replaceAll('-', '');
    if (normalized.length != 13 || int.tryParse(normalized) == null) {
      return false;
    }

    final provincePrefixStr = normalized.substring(0, 2);
    final provincePrefix = int.tryParse(provincePrefixStr);
    
    if (provincePrefix == null) return false;

    for (final range in _validProvinceDistrictRanges) {
      if (provincePrefix >= range[0] && provincePrefix <= range[1]) {
        return true;
      }
    }
    return false;
  }
}
