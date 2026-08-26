package com.qatra.app

import com.qatra.app.data.repository.QatraRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CnicValidationTest {
  @Test
  fun validPakistaniCnicFormatAndDistrictCode_shouldPassValidation() {
    assertTrue(QatraRepository.validatePakistaniCnic("42101-1234567-1".replace("-", "")))
  }

  @Test
  fun wrongLengthCnic_shouldFailValidation() {
    assertFalse(QatraRepository.validatePakistaniCnic("421011234567"))
  }

  @Test
  fun nonNumericCnic_shouldFailValidation() {
    assertFalse(QatraRepository.validatePakistaniCnic("42101-1234567-A".replace("-", "")))
  }

  @Test
  fun outOfRangeDistrictCode_shouldFailValidation() {
    assertFalse(QatraRepository.validatePakistaniCnic("9010112345678"))
  }
}
