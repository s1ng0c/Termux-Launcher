package com.termux.shared.models.errors;

/**
 * The {@link Class} that defines FileUtils error messages and codes.
 */
public class FileUtilsErrno extends Errno {

    public static final String TYPE = "FileUtils Error";


    /* Errors for null or empty paths (100-150) */
    public static final Errno ERRNO_EXECUTABLE_REQUIRED = new Errno(TYPE, 100, "Executable required.");
    public static final Errno ERRNO_NULL_OR_EMPTY_REGULAR_FILE_PATH = new Errno(TYPE, 101, "The regular file path is null or empty.");
    public static final Errno ERRNO_NULL_OR_EMPTY_REGULAR_FILE = new Errno(TYPE, 102, "The regular file is null or empty.");
    public static final Errno ERRNO_NULL_OR_EMPTY_EXECUTABLE_FILE_PATH = new Errno(TYPE, 103, "The executable file path is null or empty.");
    public static final Errno ERRNO_NULL_OR_EMPTY_EXECUTABLE_FILE = new Errno(TYPE, 104, "The executable file is null or empty.");
    public static final Errno ERRNO_NULL_OR_EMPTY_DIRECTORY_FILE_PATH = new Errno(TYPE, 105, "The directory file path is null or empty.");
    public static final Errno ERRNO_NULL_OR_EMPTY_DIRECTORY_FILE = new Errno(TYPE, 106, "The directory file is null or empty.");


    /* Errors for invalid or not found files at path (150-200) */
    public static final Errno ERRNO_FILE_NOT_FOUND_AT_PATH = new Errno(TYPE, 150, "The %1$s is not found at path \"%2$s\".");

    public static final Errno ERRNO_NO_REGULAR_FILE_FOUND = new Errno(TYPE, 151, "Regular file not found at %1$s path.");
    public static final Errno ERRNO_NOT_A_REGULAR_FILE = new Errno(TYPE, 152, "The %1$s at path \"%2$s\" is not a regular file.");

    public static final Errno ERRNO_NON_REGULAR_FILE_FOUND = new Errno(TYPE, 153, "Non-regular file found at %1$s path.");
    public static final Errno ERRNO_NON_DIRECTORY_FILE_FOUND = new Errno(TYPE, 154, "Non-directory file found at %1$s path.");
    public static final Errno ERRNO_NON_SYMLINK_FILE_FOUND = new Errno(TYPE, 155, "Non-symlink file found at %1$s path.");

    public static final Errno ERRNO_FILE_NOT_AN_ALLOWED_FILE_TYPE = new Errno(TYPE, 156, "The %1$s found at path \"%2$s\" is not one of allowed file types \"%3$s\".");

    public static final Errno ERRNO_VALIDATE_FILE_EXISTENCE_AND_PERMISSIONS_FAILED_WITH_EXCEPTION = new Errno(TYPE, 157, "Validating file existence and permissions of %1$s at path \"%2$s\" failed.\nException: %3$s");
    public static final Errno ERRNO_VALIDATE_DIRECTORY_EXISTENCE_AND_PERMISSIONS_FAILED_WITH_EXCEPTION = new Errno(TYPE, 158, "Validating directory existence and permissions of %1$s at path \"%2$s\" failed.\nException: %3$s");


    /* Errors for file creation (200-250) */
    public static final Errno ERRNO_CREATING_FILE_FAILED = new Errno(TYPE, 200, "Creating %1$s at path \"%2$s\" failed.");
    public static final Errno ERRNO_CREATING_FILE_FAILED_WITH_EXCEPTION = new Errno(TYPE, 201, "Creating %1$s at path \"%2$s\" failed.\nException: %3$s");

    public static final Errno ERRNO_CANNOT_OVERWRITE_A_NON_SYMLINK_FILE_TYPE = new Errno(TYPE, 202, "Cannot overwrite %1$s while creating symlink at \"%2$s\" to \"%3$s\" since destination file type \"%4$s\" is not a symlink.");
    public static final Errno ERRNO_CREATING_SYMLINK_FILE_FAILED_WITH_EXCEPTION = new Errno(TYPE, 203, "Creating %1$s at path \"%2$s\" to \"%3$s\" failed.\nException: %4$s");


    /* Errors for file copying and moving (250-300) */
    public static final Errno ERRNO_COPYING_OR_MOVING_FILE_FAILED_WITH_EXCEPTION = new Errno(TYPE, 250, "%1$s from \"%2$s\" to \"%3$s\" failed.\nException: %4$s");
    public static final Errno ERRNO_COPYING_OR_MOVING_FILE_TO_SAME_PATH = new Errno(TYPE, 251, "%1$s from \"%2$s\" to \"%3$s\" cannot be done since they point to the same path.");
    public static final Errno ERRNO_CANNOT_OVERWRITE_A_DIFFERENT_FILE_TYPE = new Errno(TYPE, 252, "Cannot overwrite %1$s while %2$s it from \"%3$s\" to \"%4$s\" since destination file type \"%5$s\" is different from source file type \"%6$s\".");
    public static final Errno ERRNO_CANNOT_MOVE_DIRECTORY_TO_SUB_DIRECTORY_OF_ITSELF = new Errno(TYPE, 253, "Cannot move %1$s from \"%2$s\" to \"%3$s\" since destination is a subdirectory of the source.");


    /* Errors for file deletion (300-350) */
    public static final Errno ERRNO_DELETING_FILE_FAILED = new Errno(TYPE, 300, "Deleting %1$s at path \"%2$s\" failed.");
    public static final Errno ERRNO_DELETING_FILE_FAILED_WITH_EXCEPTION = new Errno(TYPE, 301, "Deleting %1$s at path \"%2$s\" failed.\nException: %3$s");
    public static final Errno ERRNO_CLEARING_DIRECTORY_FAILED_WITH_EXCEPTION = new Errno(TYPE, 302, "Clearing %1$s at path \"%2$s\" failed.\nException: %3$s");
    public static final Errno ERRNO_FILE_STILL_EXISTS_AFTER_DELETING = new Errno(TYPE, 303, "The %1$s still exists after deleting it from \"%2$s\".");


    /* Errors for file reading and writing (350-400) */
    public static final Errno ERRNO_READING_STRING_TO_FILE_FAILED_WITH_EXCEPTION = new Errno(TYPE, 350, "Reading string from %1$s at path \"%2$s\" failed.\nException: %3$s");
    public static final Errno ERRNO_WRITING_STRING_TO_FILE_FAILED_WITH_EXCEPTION = new Errno(TYPE, 351, "Writing string to %1$s at path \"%2$s\" failed.\nException: %3$s");
    public static final Errno ERRNO_UNSUPPORTED_CHARSET = new Errno(TYPE, 352, "Unsupported charset \"%1$s\"");
    public static final Errno ERRNO_CHECKING_IF_CHARSET_SUPPORTED_FAILED = new Errno(TYPE, 353, "Checking if charset \"%1$s\" is supported failed.\nException: %2$s");


    /* Errors for invalid file permissions (400-450) */
    public static final Errno ERRNO_INVALID_FILE_PERMISSIONS_STRING_TO_CHECK = new Errno(TYPE, 400, "The file permission string to check is invalid.");
    public static final Errno ERRNO_FILE_NOT_READABLE = new Errno(TYPE, 401, "The %1$s at path is not readable. Permission Denied.");
    public static final Errno ERRNO_FILE_NOT_WRITABLE = new Errno(TYPE, 402, "The %1$s at path is not writable. Permission Denied.");
    public static final Errno ERRNO_FILE_NOT_EXECUTABLE = new Errno(TYPE, 403, "The %1$s at path is not executable. Permission Denied.");


    FileUtilsErrno(final String type, final int code, final String message) {
        super(type, code, message);
    }

}
