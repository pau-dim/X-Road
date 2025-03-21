@SoftToken
@EC
Feature: 0120 - Signer: SoftToken: Key operations (EC)

  Background:
    Given tokens are listed

  Scenario: Previous Keys are deleted
    When token "soft-token-000" has exact keys "First key,Second key,KeyX,SignKey from CA,AuthKey from CA"
    Then key "First key" is deleted from token "soft-token-000"
    And key "Second key" is deleted from token "soft-token-000"
    And key "KeyX" is deleted from token "soft-token-000"
    And key "SignKey from CA" is deleted from token "soft-token-000"
    And key "AuthKey from CA" is deleted from token "soft-token-000"

  Scenario: Keys are generated
    When new EC key "key-1" generated for token "soft-token-000"
    And name "First key" is set for generated key
    When new EC key "key-2" generated for token "soft-token-000"
    And name "Second key" is set for generated key
    When new EC key "key-3" generated for token "soft-token-000"
    And name "Third key" is set for generated key
    Then token "soft-token-000" has exact keys "First key,Second key,Third key"
    And sign mechanism for token "soft-token-000" key "Second key" is not null

  Scenario: Key is deleted
    Given new EC key "key-X" generated for token "soft-token-000"
    And name "KeyX" is set for generated key
    Then token info can be retrieved by key id
    When key "Third key" is deleted from token "soft-token-000"
    Then token "soft-token-000" has exact keys "First key,Second key,KeyX"

  Scenario: A key with Sign certificate is created
    Given new EC key "key-10" generated for token "soft-token-000"
    And name "SignKey from CA" is set for generated key
    And token "soft-token-000" has exact keys "First key,Second key,KeyX,SignKey from CA"
    And sign mechanism for token "soft-token-000" key "SignKey from CA" is not null
    When the SIGNING cert request is generated for token "soft-token-000" key "SignKey from CA" for client "DEV:COM:1234:MANAGEMENT"
    And SIGN CSR is processed by test CA
    And Generated certificate with initial status "registered" is imported for client "DEV:COM:1234:MANAGEMENT"
    Then token info can be retrieved by key id

  Scenario: A key with Auth certificate is created
    Given new EC key "key-20" generated for token "soft-token-000"
    And name "AuthKey from CA" is set for generated key
    And token "soft-token-000" has exact keys "First key,Second key,KeyX,SignKey from CA,AuthKey from CA"
    And sign mechanism for token "soft-token-000" key "AuthKey from CA" is not null
    When the AUTHENTICATION cert request is generated for token "soft-token-000" key "AuthKey from CA" for client "DEV:COM:1234:MANAGEMENT"
    And CSR is processed by test CA
    And Generated certificate with initial status "registered" is imported for client "DEV:COM:1234:MANAGEMENT"
    Then token info can be retrieved by key id

  Scenario: Sign fails with an unknown algorithm error
    Given digest can be signed using key "KeyX" from token "soft-token-000"
    And Signing with unknown algorithm fails using key "KeyX" from token "soft-token-000"

  Scenario: Generate/Regenerate cert request
    When the SIGNING cert request is generated for token "soft-token-000" key "Second key" for client "DEV:test:member-2"
    And token and key can be retrieved by cert request
    Then cert request can be deleted
    When the SIGNING cert request is generated for token "soft-token-000" key "Second key" for client "DEV:test:member-2"
    And cert request is regenerated

  Scenario: Self Signed certificate can be (re)imported
    Given tokens list contains token "soft-token-000"
    When Wrong Certificate is not imported for client "DEV:test:member-2"
    And self signed cert generated for token "soft-token-000" key "Second key", client "DEV:test:member-2"
    And certificate info can be retrieved by cert hash
    When certificate can be deleted
    Then token "soft-token-000" key "Second key" has 0 certificates
    When Certificate is imported for client "DEV:test:member-2"
    Then token "soft-token-000" key "Second key" has 1 certificates

  Scenario: Self signed certificate
    Given token "soft-token-000" key "First key" has 0 certificates
    When self signed cert generated for token "soft-token-000" key "First key", client "DEV:test:member-1"
    Then token "soft-token-000" key "First key" has 1 certificates
    And keyId can be retrieved by cert hash
    And token and keyId can be retrieved by cert hash
    And certificate can be signed using key "First key" from token "soft-token-000"

  Scenario: Member signing info can be retrieved
    Given tokens list contains token "soft-token-000"
    * Member signing info for client "DEV:COM:1234:MANAGEMENT" is retrieved

  Scenario: Member certs are retrieved
    Then member "DEV:test:member-1" has 1 certificate

  Scenario: Cert status can be updated
    Given self signed cert generated for token "soft-token-000" key "KeyX", client "DEV:test:member-2"
    And certificate info can be retrieved by cert hash
    Then certificate can be deactivated
    And certificate can be activated
    And certificate status can be changed to "deletion in progress"
    And certificate can be deleted

  Scenario: Miscellaneous checks
    * check token "soft-token-000" key "First key" batch signing enabled

  Scenario: Exceptions are being handled
    * Set token name fails with TokenNotFound exception when token does not exist
    * Deleting not existing certificate from token fails
    * Retrieving token info by not existing key fails
    * Deleting not existing certRequest fails
    * Signing with unknown key fails
    * Getting key by not existing cert hash fails
    * Not existing certificate can not be activated
    * Member signing info for client "DEV:test:member-1" fails if not suitable certificates are found
    * auth key retrieval for Security Server "DEV:COM:1234:SS100" fails when no active token is found

  Scenario: Ocsp responses
    When ocsp responses are set
    Then ocsp responses can be retrieved
    And null ocsp response is returned for unknown certificate

  Scenario: Ocsp responses verified on certificate activation
    When the SIGNING cert request is generated for token "soft-token-000" key "SignKey from CA" for client "DEV:COM:1234:MANAGEMENT"
    And SIGN CSR is processed by test CA
    And Generated certificate with initial status "registered" is imported for client "DEV:COM:1234:MANAGEMENT"
    Then certificate can be activated
    When ocsp responses are set to REVOKED
    Then certificate activation fails with ocsp verification

#  not covered SignerProxy methods:
#  AuthKeyInfo getAuthKey(SecurityServerId serverId)                            #requires valid ocsp response
