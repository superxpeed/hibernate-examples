package com.jpa.example;

import com.jpa.example.postgres.manytomany.Employee;
import com.jpa.example.postgres.manytomany.Project;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

@SuppressWarnings("all")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ManyToManyExample {

    private static EntityManager postgresEntityManager;

    private static EntityManagerFactory postgresEntityManagerFactory;

    private static final String createTableEmployee = "CREATE TABLE IF NOT EXISTS TEST.EMPLOYEE ( " +
                                                      "    EMPLOYEE_ID SERIAL PRIMARY KEY, " +
                                                      "    FIRST_NAME VARCHAR(50) DEFAULT NULL, " +
                                                      "    LAST_NAME VARCHAR(50) DEFAULT NULL) ";

    private static final String createTableProject = "CREATE TABLE IF NOT EXISTS TEST.PROJECT ( " +
                                                     "    PROJECT_ID SERIAL PRIMARY KEY, " +
                                                     "    TITLE VARCHAR(50) DEFAULT NULL)";

    private static final String createTableProjectEmployee = "CREATE TABLE IF NOT EXISTS TEST.EMPLOYEE_PROJECT ( " +
                                                             "    EMPLOYEE_ID INTEGER NOT NULL, " +
                                                             "    PROJECT_ID INTEGER NOT NULL, " +
                                                             "    PRIMARY KEY (EMPLOYEE_ID, PROJECT_ID), " +
                                                             "    CONSTRAINT employee_project_ibfk_1 " +
                                                             "    FOREIGN KEY (EMPLOYEE_ID) REFERENCES TEST.EMPLOYEE(EMPLOYEE_ID) MATCH FULL ON UPDATE NO ACTION ON DELETE NO ACTION, " +
                                                             "    CONSTRAINT employee_project_ibfk_2 " +
                                                             "    FOREIGN KEY (PROJECT_ID) REFERENCES TEST.PROJECT(PROJECT_ID) MATCH FULL ON UPDATE NO ACTION ON DELETE NO ACTION)";
    @BeforeClass
    public static void prepareEverything() {
        postgresEntityManagerFactory = Persistence.createEntityManagerFactory("postgres-persistence");
        postgresEntityManager = postgresEntityManagerFactory.createEntityManager();

        assertTrue(postgresEntityManager.isOpen());

        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.createNativeQuery("DROP TABLE IF EXISTS TEST.EMPLOYEE_PROJECT").executeUpdate();
        postgresEntityManager.createNativeQuery("DROP TABLE IF EXISTS TEST.EMPLOYEE").executeUpdate();
        postgresEntityManager.createNativeQuery("DROP TABLE IF EXISTS TEST.PROJECT").executeUpdate();
        postgresEntityManager.getTransaction().commit();

        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.createNativeQuery(createTableEmployee).executeUpdate();
        postgresEntityManager.createNativeQuery(createTableProject).executeUpdate();
        postgresEntityManager.createNativeQuery(createTableProjectEmployee).executeUpdate();
        postgresEntityManager.getTransaction().commit();
    }

    @Test
    public void stage1_insertBoth() {
        Project project = new Project();
        project.setTitle("Project 1");

        // separate set for each employee because
        // IF you use ONE set then adding new project to it
        // then updating ONE employee will add new project to ALL employees that have reference to this set
        // (that is will add new record to employee_project table for ALL employees)
        Set<Project> projects1 = new HashSet<>();
        projects1.add(project);

        Set<Project> projects2 = new HashSet<>();
        projects2.add(project);

        Set<Employee> employees = new HashSet<>();

        Employee employee1 = new Employee();
        employee1.setFirstName("First name 1");
        employee1.setLastName("Last name 1");
        employee1.setProjects(projects1);

        Employee employee2 = new Employee();
        employee2.setFirstName("First name 2");
        employee2.setLastName("Last name 2");
        employee2.setProjects(projects2);

        employees.add(employee1);
        employees.add(employee2);

        project.setEmployees(employees);

        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.persist(employee1);
        postgresEntityManager.getTransaction().commit();

        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.persist(employee2);
        postgresEntityManager.getTransaction().commit();

        project.setTitle("Project 1 updated");

        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.merge(project);
        postgresEntityManager.flush();
        postgresEntityManager.getTransaction().commit();

        Project project2 = new Project();
        project2.setTitle("Project 2");
        employee1.getProjects().add(project2);

        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.merge(employee1);
        postgresEntityManager.flush();
        postgresEntityManager.getTransaction().commit();

        Long employeeId1 = employee1.getEmployeeId();
        Long employeeId2 = employee2.getEmployeeId();

        postgresEntityManager.clear();

        Employee employee11 = postgresEntityManager.find(Employee.class, employeeId1);
        Employee employee22 = postgresEntityManager.find(Employee.class, employeeId2);

        assertTrue(employee11.getProjects().size() == 2);
        assertTrue(employee22.getProjects().size() == 1);
        assertTrue(((Project)employee22.getProjects().toArray()[0]).getTitle().equals("Project 1 updated"));

        // now let's try to add new Project to all Employees
        Project project3 = new Project();
        project3.setTitle("Project 3");

        // single set for ALL employees
        Set<Project> projects3 = new HashSet<>();
        projects3.add(project3);

        Set<Employee> employees2 = new HashSet<>();

        Employee employee3 = new Employee();
        employee3.setFirstName("First name 3");
        employee3.setLastName("Last name 3");
        employee3.setProjects(projects3);

        Employee employee4 = new Employee();
        employee4.setFirstName("First name 4");
        employee4.setLastName("Last name 4");
        employee4.setProjects(projects3);

        employees2.add(employee4);
        employees2.add(employee3);

        project3.setEmployees(employees2);

        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.persist(employee3);
        postgresEntityManager.getTransaction().commit();

        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.persist(employee4);
        postgresEntityManager.getTransaction().commit();

        Project project4 = new Project();
        project4.setTitle("Project 4");
        employee3.getProjects().add(project4);

        // update only ONE entity
        postgresEntityManager.getTransaction().begin();
        employee3 = postgresEntityManager.merge(employee3);
        postgresEntityManager.flush();
        postgresEntityManager.getTransaction().commit();

        // merge doesn't make project4 managed so we need to add it to EntityManager, but employee3 is updated
        // so project4.getProjectId() returns null
        System.out.println("After project4 insertion: " + project4.getProjectId());

        // but employee3's Project set contains only managed entities, so let's find it
        project4 = employee3.getProjects().stream().filter(x -> x.getTitle().equals("Project 4")).findFirst().get();

        Long employeeId3 = employee3.getEmployeeId();
        Long employeeId4 = employee4.getEmployeeId();

        Long projectId3 = project3.getProjectId();
        Long projectId4 = project4.getProjectId();

        postgresEntityManager.clear();

        Employee employee33 = postgresEntityManager.find(Employee.class, employeeId3);
        Employee employee44 = postgresEntityManager.find(Employee.class, employeeId4);

        Project project33 = postgresEntityManager.find(Project.class, projectId3);
        Project project44 = postgresEntityManager.find(Project.class, projectId4);

        assertTrue(employee33.getProjects().size() == 2);
        assertTrue(employee44.getProjects().size() == 2);

        assertTrue(project33.getEmployees().size() == 2);
        assertTrue(project44.getEmployees().size() == 2);

    }
}
