class UserAlreadyExistsException() : Exception("User already exists")
class CannotGetUserRoleException() : Exception("Cannot get user role. The user's phone number is not registered by admin.")