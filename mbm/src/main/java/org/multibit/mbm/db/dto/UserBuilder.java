package org.multibit.mbm.db.dto;

import com.google.common.collect.Lists;
import org.jasypt.util.password.StrongPasswordEncryptor;

import java.util.List;
import java.util.UUID;

/**
 *  <p>Builder to provide the following to {@link org.multibit.mbm.db.dto.User}:</p>
 *  <ul>
 *  <li>Provide a fluent interface to facilitate building the entity</li>
 *  </ul>
 *
 * @since 0.0.1
 *         
 */
public class UserBuilder {

  private String openId;
  private String uuid = UUID.randomUUID().toString();
  private String secretKey;
  private List<AddContactMethod> addContactMethods = Lists.newArrayList();
  private List<AddRole> addRoles = Lists.newArrayList();
  private String username;
  private String password;
  private Customer customer;

  private boolean isBuilt = false;

  /**
   * @return A new instance of the builder
   */
  public static UserBuilder newInstance() {
    return new UserBuilder();
  }

  /**
   * Handles the building process. No further configuration is possible after this.
   */
  public User build() {
    validateState();

    // User is a DTO and so requires a default constructor
    User user = new User();

    user.setOpenId(openId);

    if (uuid == null) {
      throw new IllegalStateException("UUID cannot be null");
    }
    user.setUUID(uuid);

    user.setSecretKey(secretKey);
    user.setUsername(username);

    if (password != null) {
      // Digest the plain password
      String encryptedPassword = new StrongPasswordEncryptor().encryptPassword(password);
      user.setPassword(encryptedPassword);
    }

    // Bi-directional relationship
    if (customer != null) {
      user.setCustomer(customer);
      customer.setUser(user);
    }

    for (AddRole addRole : addRoles) {
      addRole.applyTo(user);
    }

    for (AddContactMethod addContactMethod : addContactMethods) {
      addContactMethod.applyTo(user);
    }

    isBuilt = true;

    return user;
  }

  private void validateState() {
    if (isBuilt) {
      throw new IllegalStateException("The entity has been built");
    }
  }

  /**
   * @param openId The openId (e.g. "abc123")
   *
   * @return The builder
   */
  public UserBuilder withOpenId(String openId) {
    this.openId = openId;
    return this;
  }

  /**
   * @param uuid The UUID (e.g. "1234-5678")
   *
   * @return The builder
   */
  public UserBuilder withUUID(String uuid) {
    this.uuid = uuid;
    return this;
  }

  /**
   * @param secretKey The secretKey (base64 encoded)
   *
   * @return The builder
   */
  public UserBuilder withSecretKey(String secretKey) {
    this.secretKey = secretKey;
    return this;
  }

  public UserBuilder withContactMethod(ContactMethod contactMethod, String detail) {

    addContactMethods.add(new AddContactMethod(contactMethod, detail));

    return this;
  }

  public UserBuilder withRole(Role role) {

    addRoles.add(new AddRole(role));
    return this;
  }

  public UserBuilder withRoles(List<Role> roles) {

    for (Role role : roles) {
      addRoles.add(new AddRole(role));
    }

    return this;
  }

  public UserBuilder withUsername(String username) {
    this.username = username;
    return this;
  }

  public UserBuilder withPassword(String password) {
    this.password = password;
    return this;
  }

  /**
   * Add the Customer to the User (one permitted)
   *
   * @return The builder
   */
  public UserBuilder withCustomer(Customer customer) {
    this.customer = customer;
    return this;
  }

  /**
   * Handles adding a new contact method to the user
   */
  private class AddContactMethod {
    private final ContactMethod contactMethod;
    private final String detail;

    private AddContactMethod(ContactMethod contactMethod, String detail) {
      this.contactMethod = contactMethod;
      this.detail = detail;
    }

    void applyTo(User user) {
      ContactMethodDetail contactMethodDetail = new ContactMethodDetail();
      contactMethodDetail.setPrimaryDetail(detail);

      user.setContactMethodDetail(contactMethod, contactMethodDetail);

    }
  }

  /**
   * Handles adding a new contact method to the user
   */
  private class AddRole {
    private final Role role;

    private AddRole(Role role) {
      this.role = role;
    }

    void applyTo(User user) {

      UserRole userRole = new UserRole();

      UserRole.UserRolePk userRolePk = new UserRole.UserRolePk();
      userRolePk.setUser(user);
      userRolePk.setRole(role);

      userRole.setPrimaryKey(userRolePk);

      user.getUserRoles().add(userRole);

    }
  }

}
