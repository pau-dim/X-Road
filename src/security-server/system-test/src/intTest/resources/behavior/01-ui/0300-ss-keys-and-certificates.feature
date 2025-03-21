@SecurityServer
@Initialization
Feature: 0300 - SS: Keys and certificates

  Background:
    Given SecurityServer login page is open
    And Page is prepared to be tested
    And User xrd logs in to SecurityServer with password secret
    And signer service is restarted

  Scenario Outline: <$label> key is added and imported
    Given healthcheck has errors and error message is "No certificate chain available in authentication key."
    And Keys and certificates tab is selected
    And Token: <$token> is present and expanded
    When Token: <$token> - Add key wizard is opened
    And Key Label is set to "<$label>"
    And CSR details Usage is set to "<$usage>", Client set to "<$client>", Certification Service to "<$certService>" and CSR format "PEM"
    And Generate "<$usage>" CSR is set to DNS "<$dns>" and Organization "ui-test"
    And CSR with extension "pem" successfully generated
    And Token: <$token> - has key with label "<$label>"
    Then CSR is processed by test CA
    And Token: <$token> - Generated certificate is imported
    And Token: <$token> - has key "<$label>" with status "<$certStatus>" and ocsp status "<$ocspStatus>"
    And Token: <$token> - has "<$usage>" key "<$label>" with correct fixed automatic renewal status
    And Token: <$token>, key "<$label>" generate CSR button is disabled
    Examples:
      | $token      | $usage         | $label             | $client      | $dns  | $certService | $certStatus | $ocspStatus |
      | softToken-0 | SIGNING        | test signing key   | DEV:COM:1234 | ss0   | Test CA      | Registered  | Good        |
      | softToken-0 | AUTHENTICATION | test auth key      |              | ss0   | Test CA      | Saved       | Disabled    |

  Scenario: Token edit page is navigable
    Given Keys and certificates tab is selected
    When Token: softToken-0 edit page is opened
    Then Token Alert about token policy being enforced is present

  Scenario Outline: New key with with empty label is created
    Given Keys and certificates tab is selected
    And Token: <$token> is present and expanded
    When Token: <$token> - Add key wizard is opened
    And Key Label is set to "<$label>"
    And CSR details Usage is set to "<$usage>", Client set to "<$client>", Certification Service to "<$certService>" and CSR format "PEM"
    And Generate "<$usage>" CSR is set to DNS "<$dns>" and Organization "ui-test"
    And CSR with extension "pem" successfully generated
    And Token: <$token> - has <$authKeyAmount> auth keys, <$signKeyAmount> sign keys
    Examples:
      | $token      | $usage         | $label | $client      | $dns | $certService | $authKeyAmount | $signKeyAmount |
      | softToken-0 | SIGNING        |        | DEV:COM:1234 | ss0  | Test CA      | 1              | 2              |
      | softToken-0 | AUTHENTICATION |        |              | ss0  | Test CA      | 2              | 2              |

  Scenario: Add key wizard is navigable
    Given Keys and certificates tab is selected
    And Token: softToken-0 is present and expanded
    When Token: softToken-0 - Add key wizard is opened
    Then Add key wizard is closed
    When Token: softToken-0 - Add key wizard is opened
    And Key Label is set to ""
    And CSR details Usage is set to "AUTHENTICATION", Client set to "", Certification Service to "Test CA" and CSR format "DER"
    And Add key wizard Generate CSR step is closed
    And Token: softToken-0 - has 2 auth keys, 2 sign keys

  Scenario: CSR can be deleted
    Given Keys and certificates tab is selected
    And Token: softToken-0 is present and expanded
    When Token: softToken-0 - "AUTHENTICATION" CSR in position 1 is deleted
    Then Token: softToken-0 - has 1 auth keys, 2 sign keys
    When Token: softToken-0 - "SIGNING" CSR in position 1 is deleted
    Then Token: softToken-0 - has 1 auth keys, 1 sign keys

  Scenario: Generating multiple CSR for key
    Given Keys and certificates tab is selected
    And Token: softToken-0 is present and expanded
    When Token: softToken-0 - Add key wizard is opened
    And Key Label is set to "key for multiple csr"
    And CSR details Usage is set to "AUTHENTICATION", Client set to "", Certification Service to "Test CA" and CSR format "PEM"
    And Generate "AUTHENTICATION" CSR is set to DNS "ss0" and Organization "ui-test"
    And CSR with extension "pem" successfully generated
    And CSR is generated for token "softToken-0", key "key for multiple csr", certification service "Test CA", format "DER", security server "ss0"
    And CSR is generated for token "softToken-0", key "key for multiple csr", certification service "Test CA", format "DER", security server "ss0"
    And CSR is generated for token "softToken-0", key "key for multiple csr", certification service "Test CA", format "DER", security server "ss0"
    Then Token "softToken-0", key "key for multiple csr" has 4 certificate signing requests

  Scenario: Token PIN can be changed
    Given Keys and certificates tab is selected
    And Token: softToken-0 edit page is opened
    And Change the pin section is expanded
    When PIN is changed from "T0ken1zer3" to "T0ken1zer3New"
    Then Token: softToken-0 is logged-out
    And User logs in token: softToken-0 with PIN: T0ken1zer3New
    And Token: softToken-0 is logged-in
    When Token: softToken-0 edit page is opened
    And Change the pin section is expanded
    And PIN is changed from "T0ken1zer3New" to "T0ken1zer3"
    Then Token: softToken-0 is logged-out
    And User logs in token: softToken-0 with PIN: T0ken1zer3
    And Token: softToken-0 is logged-in

  Scenario: Inactive token can be deleted
    Given Predefined inactive signer token is uploaded
    And Keys and certificates tab is selected
    When Token: softToken-for-deletion edit page is opened
    Then Inactive token softToken-for-deletion is successfully deleted

