package via.sep2.client.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidationTest {

    @Test
    void testUsernameValidation_ValidUsernames() {
        assertTrue(AuthTestUtils.isValidUsername("user123"));
        assertTrue(AuthTestUtils.isValidUsername("test_user"));
        assertTrue(AuthTestUtils.isValidUsername("User_Name_123"));
        assertTrue(AuthTestUtils.isValidUsername("abc"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "ab", "a", "", "user@name", "user-name", "user.name", "user name" })
    void testUsernameValidation_InvalidUsernames(String username) {
        assertFalse(AuthTestUtils.isValidUsername(username));
    }

    @Test
    void testPasswordValidation_ValidPasswords() {
        assertTrue(AuthTestUtils.isValidPassword("password123"));
        assertTrue(AuthTestUtils.isValidPassword("12345678"));
        assertTrue(AuthTestUtils.isValidPassword("VeryLongPassword123!@#"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1234567", "abc", "", "short" })
    void testPasswordValidation_InvalidPasswords(String password) {
        assertFalse(AuthTestUtils.isValidPassword(password));
    }

    @Test
    void testNameValidation_ValidNames() {
        assertTrue(AuthTestUtils.isValidName("John"));
        assertTrue(AuthTestUtils.isValidName("Mary Jane"));
        assertTrue(AuthTestUtils.isValidName("O'Connor"));
        assertTrue(AuthTestUtils.isValidName("Smith-Jones"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   ", "123", "John123", "Name@email", "User_Name" })
    void testNameValidation_InvalidNames(String name) {
        assertFalse(AuthTestUtils.isValidName(name));
    }

    @ParameterizedTest
    @CsvSource({
            "testuser, password123, Test, User, true",
            "ab, password123, Test, User, false",
            "testuser, 123, Test, User, false",
            "testuser, password123, '', User, false",
            "testuser, password123, Test, '', false",
            "'', password123, Test, User, false"
    })
    void testCompleteFormValidation(String username, String password, String firstName, String lastName,
            boolean expected) {
        boolean isValid = AuthTestUtils.isValidUsername(username) &&
                AuthTestUtils.isValidPassword(password) &&
                AuthTestUtils.isValidName(firstName) &&
                AuthTestUtils.isValidName(lastName);

        assertEquals(expected, isValid);
    }
}
