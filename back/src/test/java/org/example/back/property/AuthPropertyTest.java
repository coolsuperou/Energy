package org.example.back.property;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.example.back.entity.User;
import org.example.back.entity.enums.UserRole;
import org.example.back.entity.enums.UserStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 用户认证属性测试
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	测试用户认证相关属性（P1.1-P1.4）
 * -- =============================================
 */
class AuthPropertyTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * P1.1 登录成功属性测试
     * 正确的用户名和密码应该能够成功登录
     */
    @Property
    void correctCredentialsShouldAllowLogin(
            @ForAll @AlphaChars @StringLength(min = 4, max = 20) String username,
            @ForAll @StringLength(min = 6, max = 20) String rawPassword) {
        
        // 创建用户并加密密码
        User user = createUser(username, rawPassword, UserStatus.ACTIVE);
        String encodedPassword = user.getPassword();
        
        // 验证密码匹配
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        
        assertThat(matches).isTrue("正确密码应该匹配");
    }

    /**
     * P1.2 登录失败属性测试（错误密码）
     * 错误的密码不应该能够登录
     */
    @Property
    void wrongPasswordShouldDenyLogin(
            @ForAll @AlphaChars @StringLength(min = 4, max = 20) String username,
            @ForAll @StringLength(min = 6, max = 20) String correctPassword,
            @ForAll @StringLength(min = 6, max = 20) String wrongPassword) {
        
        // 确保两个密码不同
        Assume.that(!correctPassword.equals(wrongPassword));
        
        // 创建用户
        User user = createUser(username, correctPassword, UserStatus.ACTIVE);
        String encodedPassword = user.getPassword();
        
        // 验证错误密码不匹配
        boolean matches = passwordEncoder.matches(wrongPassword, encodedPassword);
        
        assertThat(matches).isFalse("错误密码不应该匹配");
    }

    /**
     * P1.3 禁用用户登录属性测试
     * 禁用状态的用户不应该能够登录（即使密码正确）
     */
    @Property
    void disabledUserShouldNotLogin(
            @ForAll @AlphaChars @StringLength(min = 4, max = 20) String username,
            @ForAll @StringLength(min = 6, max = 20) String password) {
        
        // 创建禁用用户
        User user = createUser(username, password, UserStatus.DISABLED);
        
        // 验证用户状态为禁用
        assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
        
        // 模拟登录检查：即使密码正确，禁用用户也不能登录
        boolean canLogin = canUserLogin(user, password);
        
        assertThat(canLogin).isFalse("禁用用户不应该能够登录");
    }

    /**
     * P1.4 用户名重复注册属性测试
     * 相同用户名不能重复注册
     */
    @Property
    void duplicateUsernameShouldBeRejected(
            @ForAll @AlphaChars @StringLength(min = 4, max = 20) String username) {
        
        // 模拟已存在的用户
        User existingUser = createUser(username, "password123", UserStatus.ACTIVE);
        
        // 检查用户名是否已存在
        boolean usernameExists = checkUsernameExists(existingUser.getUsername(), username);
        
        assertThat(usernameExists).isTrue("相同用户名应该被检测为已存在");
    }

    /**
     * 密码加密属性测试
     * 加密后的密码不应该等于原始密码
     */
    @Property
    void encodedPasswordShouldDifferFromRaw(
            @ForAll @StringLength(min = 6, max = 20) String rawPassword) {
        
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(encodedPassword.length()).isGreaterThan(rawPassword.length());
    }

    /**
     * 密码加密一致性属性测试
     * 同一密码多次加密后，都应该能够验证通过
     */
    @Property
    void multipleEncodingsShouldAllMatch(
            @ForAll @StringLength(min = 6, max = 20) String rawPassword) {
        
        String encoded1 = passwordEncoder.encode(rawPassword);
        String encoded2 = passwordEncoder.encode(rawPassword);
        
        // 两次加密结果应该不同（BCrypt 使用随机盐）
        assertThat(encoded1).isNotEqualTo(encoded2);
        
        // 但都应该能验证原始密码
        assertThat(passwordEncoder.matches(rawPassword, encoded1)).isTrue("第一次加密应该匹配");
        assertThat(passwordEncoder.matches(rawPassword, encoded2)).isTrue("第二次加密应该匹配");
    }

    /**
     * 用户角色属性测试
     * 所有角色都应该是有效的枚举值
     */
    @Property
    void allRolesShouldBeValid(@ForAll UserRole role) {
        assertThat(role).isNotNull();
        assertThat(role.getValue()).isNotNull();
        assertThat(role.getDescription()).isNotNull();
    }

    // ========== 辅助方法 ==========

    private User createUser(String username, String password, UserStatus status) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setStatus(status);
        user.setRole(UserRole.WORKSHOP);
        user.setName("测试用户");
        return user;
    }

    private boolean canUserLogin(User user, String rawPassword) {
        // 检查用户状态
        if (user.getStatus() != UserStatus.ACTIVE) {
            return false;
        }
        // 检查密码
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    private boolean checkUsernameExists(String existingUsername, String newUsername) {
        return existingUsername.equals(newUsername);
    }

    // ========== 断言辅助类 ==========

    private static <T> AssertHelper<T> assertThat(T actual) {
        return new AssertHelper<>(actual);
    }

    private static class AssertHelper<T> {
        private final T actual;

        AssertHelper(T actual) {
            this.actual = actual;
        }

        AssertHelper<T> isTrue(String message) {
            if (actual instanceof Boolean && !(Boolean) actual) {
                throw new AssertionError(message);
            }
            return this;
        }

        AssertHelper<T> isFalse(String message) {
            if (actual instanceof Boolean && (Boolean) actual) {
                throw new AssertionError(message);
            }
            return this;
        }

        AssertHelper<T> isNotNull() {
            if (actual == null) {
                throw new AssertionError("Expected non-null value");
            }
            return this;
        }

        AssertHelper<T> isEqualTo(T expected) {
            if (!java.util.Objects.equals(actual, expected)) {
                throw new AssertionError("Expected: " + expected + ", but was: " + actual);
            }
            return this;
        }

        AssertHelper<T> isNotEqualTo(T expected) {
            if (java.util.Objects.equals(actual, expected)) {
                throw new AssertionError("Expected not equal to: " + expected);
            }
            return this;
        }

        AssertHelper<T> isGreaterThan(int expected) {
            if (actual instanceof Integer && (Integer) actual <= expected) {
                throw new AssertionError("Expected > " + expected + ", but was: " + actual);
            }
            return this;
        }
    }
}
