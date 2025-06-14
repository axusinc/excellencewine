class UserAlreadyExistsException() : Exception("User already exists")
class NotRegisteredUserAlreadyExistsException() : Exception("Not registered user with this phone number already exists")
class CannotGetUserRoleException() : Exception("Cannot get user role. The user's phone number is not registered by admin.")