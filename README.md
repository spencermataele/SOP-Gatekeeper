*** Dear Evaluator, ***
-----

***Please use this README to assist in locating each task 3 section B requirement.
-----
●  Commit with a message and push when you complete each of the tasks listed below (e.g., parts B to E).

*** Though each task is described in one way or another, there are many commits.  So this README will help point you to examples as well.
-----

Note: You may commit and push whenever you want to back up your changes, even if a task is not complete.

B.  Design and develop a fully functional full stack (mobile or web) software product that addresses your identified business problem or organizational need. Include each of the following attributes, as they are the minimum required elements for the application:

●  B1 code including inheritance, polymorphism, and encapsulation
-----
*** INHERITANCE An example of inheritance can be found at backend/main/java/com.woven/app/repository/SopRepository.java line 10. 
                      Here you'll notice the repository is inheriting CRUD behavior from JpaRepository.  This give save(), findById(), delete(), paging, and sorting.
                Another example can be found at backend/main/java/com.woven/app/service/user/AppUserDetails.java line 11.
                      Here you will see that AppUserDetails inherits UserDetails from springframework.secuity.

*** POLYMORPHISM A good example of polymorphism can be found at backend/main/java/com.woven/app/service/ChangeRequestService.java line 413.
                      Here the findAll() method uses variable type ChangeRequestRepository changeRequestRepository.  Spring uses this as a parent
                      type and refers to a child implementation (SimpleJpaRepository) at runtime.

*** ENCAPSULATION You'll find that this app uses several DTO layers to isolate the DB from API contracts.  Refer to backend/main/java/com.woven/app/SopDto.java. 

●  B2 search functionality with multiple row results and displays
-----
*** A search tool implementation can be found at fronted/src/app/features/admin/process-owners/process-owners.componenet.ts line 125.  
      You may also navigate to the Process Owners page in the UI by selecting the Admin option in the main menu and then the Process Owners option.
      Test by searching for "John" and also by "Anderson".  You see the list filter to all rows containing those values.

●  B3 a database component with the functionality to securely add, modify, and delete the data
-----
*** An example of this can be found in backend/main/java/com.woven/app/service/user/UsersService.java lines 52-93.
    This service controls access, utilizes DTOs, and calls the repository safely in order to modify data in the database.

●  B4 ability to generate reports with multiple columns, multiple rows, date-time stamps, and title
-----
*** Refer to backend/main/java/com.woven/app/service/report/ReportService.java lines 19-111.  Also, navigate to the Reports section using the UI main menu option.

●  B5 validation functionality
-----
*** Example 1: backend/main/java/com.woven/app/dto/ProcessOwnerCreateDto.java.  This implements Jakarta validation annotation to prevent null/blank values.
    Example 2: backend/main/java/com.woven/app/service/ChangeRequestService.java lines 124-135.  You'll find validators to check a change approval id, to make sure
      the current user is a valid approver, and that the approval is in a valid status prior making modifications.

●  B6 industry-appropriate security features
-----
*** Example 1: backend/main/java/com.woven/app/config/PasswordConfig.java where you'll see that I implement BCryptPasswordEncoder() to make sure passwords are encrypted 
      and never stored in plain text.  
    Example 2: backend/main/java/com.woven/app/service/userAuth/JwtService.java line 30.  Here I use JWT authentication by using a secure token exchange with expiration.
    Example 3: backend/main/java/com.woven/app/service/ChangeRequestService.java line 174.  New SOP are not able to be published unless the current user is the original 
      SOP's process owner for data integrity and a primary governance feature.

●  B7 design elements that make the application scalable
-----
*** Example 1: This is evident in my use of layered architecture that separate responsibilities. Each process flow includes 
      Controller -> Service -> Repo -> Database.  I.e. ChangeRequestController -> ChangeRequestService -> ChangeRequestRepository -> MySQL Database.
    Example 2: I also make consistent use of DTOs that prevent over-fetching data.  I.e. SopDto, SopPublishRequestDto, ProcessOwnerCreateDto, etc.

●  B8 a user-friendly, functional GUI
-----
*** Some good examples of this can be found in frontend/src/app/features/sops/sops-form.component.ts.
      Lines 240 - 514.  As the user selects values from drop menus, other drop menus will filter their values if related so that the menu options decrease as you select parent
        options.  This also results in a quick experience if you pick the last child as each parent up the family tree will autofill.
    Also, on the corresponding html (frontend/src/app/features/sops/sops-form.component.html) page, each field includes form validators as well as style validation on required fields.
