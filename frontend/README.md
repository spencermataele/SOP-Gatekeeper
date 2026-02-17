*** Dear Evaluator, ***
-----

***Please use this README to assist in locating each task 3 requirement.
-----
A.  Create your subgroup and project by logging into GitLab using the web link provided and using the “GitLab How-To” web link, and do the following:

●  Clone the project to the IDE.

●  Commit with a message and push when you complete each of the tasks listed below (e.g., parts B to E).

*** Though each task is described in one way or another, there are many commits.  So this README will help point you to examples as well.
-----

Note: You may commit and push whenever you want to back up your changes, even if a task is not complete.

●  Submit a copy of the GitLab repository URL and a copy of the repository branch history retrieved from your repository, which must include the commit messages and dates.

Note: Wait until you have completed all the following prompts before you create your copy of the repository branch history.


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

●  design elements that make the application scalable

●  a user-friendly, functional GUI


C.  Create each of the following forms of documentation for the software product you have developed:

●  a design document including a class diagram and design diagram

●  link to where the web app is hosted with HTML code (if applicable)

●  link to the GitLab repository of the code indicating the version included in this submission

●  user guide for setting up and running the application for maintenance purposes

●  user guide for running the application from a user perspective


D.  Explain how the software product was tested, including the following:

●  a test plan for a unit test, including screenshots

●  unit test scripts

●  the results of the unit tests based on the provided test plan, including screenshots

●  summaries of changes resulting from completed tests


E.  Provide a Panopto video recording that includes a demonstration of the functionality of the software application and a summary of the tool or tools used.
