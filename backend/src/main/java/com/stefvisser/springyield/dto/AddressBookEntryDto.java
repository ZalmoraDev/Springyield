package com.stefvisser.springyield.dto;

import lombok.*;

/**
 * Data Transfer Object for address book entries.
 * <p>
 * This class encapsulates the information for an address book entry,
 * including the first name, last name, and IBAN of a contact.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressBookEntryDto {
    private String firstName;
    private String lastName;
    private String iban;

    /**
     * Static factory method to convert an AccountProfileDto to an AddressBookEntryDTO.
     *
     * @param accountProfileDto the account profile dto to convert
     * @return a new AddressBookEntryDTO with data from the account profile
     */
    public static AddressBookEntryDto wrap(AccountProfileDto accountProfileDto) {
        return new AddressBookEntryDto(
                accountProfileDto.getUser().getFirstName(),
                accountProfileDto.getUser().getLastName(),
                accountProfileDto.getIban()
        );
    }
}
