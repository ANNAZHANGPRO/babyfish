/*
 * BabyFish, Object Model Framework for Java and JPA.
 * https://github.com/babyfish-ct/babyfish
 *
 * Copyright (c) 2008-2016, Tao Chen
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * Please visit "http://opensource.org/licenses/LGPL-3.0" to know more.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 */
package org.babyfishdemo.spring.dal;

import javax.persistence.Persistence;

import junit.framework.Assert;

import org.babyfish.model.jpa.path.GetterType;
import org.babyfishdemo.spring.dal.base.AbstractRepositoryTest;
import org.babyfishdemo.spring.entities.Employee;
import org.babyfishdemo.spring.entities.Employee__;
import org.babyfishdemo.spring.model.Page;
import org.junit.Test;

/**
 * This test shows the paging query for the default query type
 * (org.babyfish.persistence.QueryType.DISTINCT)
 *
 * @author Tao Chen
 */
public class PagingQueryForDistinctModeTest extends AbstractRepositoryTest {

    @Test
    public void testSimplePagingQuery() {
        
        /*
         * For both oracle and hsqldb,
         * the simplest paging query can be applied on database level.
         */
        Page<Employee> page = 
                this.employeeRepository.getEmployees(
                        null, 
                        1, 
                        4,
                        Employee__.preOrderBy().name().firstName().asc()
                );
        
        
        /*
         * Two SQLs
         * SQL[0]: generated by org.babyfish.persistence.XQuery.getUnlimitedCount()
         * SQL[1]: generated by javax.persistence.Query.getResultList()
         */
        Assert.assertEquals(2, this.preparedSqlList.size());
        Assert.assertEquals(
                "select count(employee0_.EMPLOYEE_ID) "
                + "from EMPLOYEE employee0_",
                this.preparedSqlList.get(0)
        );
        if (this.isOracle()) {
            Assert.assertEquals(
                    "select "
                    + "* "
                    + "from ( "
                    +     "select "
                    +         "row_.*, "
                    +         "rownum rownum_ "
                    +     "from ( "
                    +         "select "
                    +             "<...many columns of employee0_...> "
                    +         "from EMPLOYEE employee0_ "
                    +         "order by employee0_.FIRST_NAME asc "
                    +     ") row_ "
                    +     "where rownum <= ?"
                    + ") "
                    + "where rownum_ > ?", 
                    this.preparedSqlList.get(1)
            );
        } else {
            Assert.assertEquals(
                    "select "
                    +     "<...many columns of employee0_...> "
                    + "from EMPLOYEE employee0_ "
                    + "order by employee0_.FIRST_NAME asc "
                    + "offset ? "
                    + "limit ?", 
                    this.preparedSqlList.get(1)
            );
        }
        
        
        Assert.assertEquals(1, page.getActualPageIndex());
        Assert.assertEquals(12, page.getTotalRowCount());
        Assert.assertEquals(3, page.getTotalPageCount());
        Assert.assertEquals(
                "[ "
                +   "{ "
                +     "name: { "
                +       "firstName: Matt, "
                +       "lastName: Horner "
                +     "}, "
                +     "gender: MALE, "
                +     "birthday: 1990-07-14, "
                +     "description: @UnloadedClob, "
                +     "image: @UnloadedBlob, "
                +     "department: @UnloadedReference, "
                +     "annualLeaves: @UnloadedCollection "
                +   "}, "
                +   "{ "
                +     "name: { "
                +       "firstName: Mohandar, "
                +       "lastName: null "
                +     "}, "
                +     "gender: MALE, "
                +     "birthday: 1423-02-17, "
                +     "description: @UnloadedClob, "
                +     "image: @UnloadedBlob, "
                +     "department: @UnloadedReference, "
                +     "annualLeaves: @UnloadedCollection "
                +   "}, "
                +   "{ "
                +     "name: { "
                +       "firstName: Nova, "
                +       "lastName: Terra "
                +     "}, "
                +     "gender: FEMALE, "
                +     "birthday: 1993-05-04, "
                +     "description: @UnloadedClob, "
                +     "image: @UnloadedBlob, "
                +     "department: @UnloadedReference, "
                +     "annualLeaves: @UnloadedCollection "
                +   "}, "
                +   "{ "
                +     "name: { "
                +       "firstName: Selendis, "
                +       "lastName: null "
                +     "}, "
                +     "gender: FEMALE, "
                +     "birthday: 1003-12-11, "
                +     "description: @UnloadedClob, "
                +     "image: @UnloadedBlob, "
                +     "department: @UnloadedReference, "
                +     "annualLeaves: @UnloadedCollection "
                +   "} "
                + "]",
                getEmployeesString(page.getEntities())
        );
    }
    
    @Test
    public void testPagingQueryByOracleDenseRank() {
        
        /*
         * For oracle: Do paging query on database-level
         * For hsqldb: Has to do paging query on memory-level
         */
        Page<Employee> page = 
                this.employeeRepository.getEmployees(
                        null, 
                        1, 
                        4,
                        Employee__.begin().annualLeaves().end(),
                        Employee__.preOrderBy().name().firstName().asc()
                );
        
        
        /*
         * Two SQLs
         * SQL[0]: generated by org.babyfish.persistence.XQuery.getUnlimitedCount()
         * SQL[1]: generated by javax.persistence.Query.getResultList()
         */
        Assert.assertEquals(2, this.preparedSqlList.size());
        /*
         * The left collection join for "employee.annualLeves" is ignored by the 
         * "org.babyfish.persistence.XQuery.getUnlimitedCount()", because
         * (1) The join type is left join(org.babyfish.model.jpa.path.GetterType.OPTINAL)
         * (2) The join is not used by any expression of where
         * (3) The query type is org.babyfish.persistence.QueryType.DISTINCT so that collection join can be ignored
         */
        Assert.assertEquals(
                "select count(employee0_.EMPLOYEE_ID) "
                + "from EMPLOYEE employee0_", 
                this.preparedSqlList.get(0)
        );
        if (this.isOracle()) {
            /*
             * Fortunately, only the columns of the root entity (Employee) is used by the 
             * "order by" clause, babyfish-hibernate can use the built-in analytic function 
             * "DENSE_RANK()" of Oracle to apply it.
             */
            Assert.assertEquals(
                    "select "
                    +     "* "
                    + "from ("
                    +     "select "
                    +         "<...many columns of employee0_...>, "
                    +         "<...many columns of annualleav1_...>, "
                    +         "dense_rank() over("
                    +             "order by "
                    +                 "employee0_.FIRST_NAME asc, "
                    +                 "employee0_.rowid asc" // this column is added by babyfish automatically
                    +         ") dense_rank____ "
                    +     "from EMPLOYEE employee0_ "
                    +     "left outer join ANNUAL_LEAVE annualleav1_ "
                    +         "on employee0_.EMPLOYEE_ID=annualleav1_.EMPLOYEE_ID "
                    + ") "
                    + "where "
                    +     "dense_rank____ <= ? "
                    + "and "
                    +     "dense_rank____ > ?", 
                    this.preparedSqlList.get(1)
            );
        } else {
            /*
             * Because this paging query contains collection fetch, hsqldb has to apply
             * it on memory level, query all the result at first, then only return the 5th, 
             * 6th, 7th and 8th rows.
             *     
             * Be different with hibernate, acquiescently, memory-level paging query is 
             * disabled in babyfish-hibernate, so you must use the babyfish-hibernate property
             * "babyfish.hibernate.enable_limit_in_memory" to enable it.
             * Please see "src/main/resources/data-access-layer.xml" to know more.
             */
            Assert.assertEquals(
                    "select "
                    +     "<...many columns of employee0_...>, "
                    +     "<...many columns of annualleav1_...> "
                    + "from EMPLOYEE employee0_ "
                    + "left outer join ANNUAL_LEAVE annualleav1_ "
                    +     "on employee0_.EMPLOYEE_ID=annualleav1_.EMPLOYEE_ID "
                    + "order by employee0_.FIRST_NAME asc", 
                    this.preparedSqlList.get(1)
            );
        }
        
        
        Assert.assertEquals(1, page.getActualPageIndex());
        Assert.assertEquals(12, page.getTotalRowCount());
        Assert.assertEquals(3, page.getTotalPageCount());
        
        /*
         * Theoretically, the order of the collection "annualLeaves" of each Employee object
         * is unknown, so we don't do so detail assertion here. We only check the firstName
         * and annualLeave count of each employee object.
         */
        Assert.assertEquals(4, page.getEntities().size());
        
        Assert.assertEquals("Matt", page.getEntities().get(0).getName().getFirstName());
        Assert.assertTrue(Persistence.getPersistenceUtil().isLoaded(page.getEntities().get(0).getAnnualLeaves()));
        Assert.assertEquals(2, page.getEntities().get(0).getAnnualLeaves().size());
        
        Assert.assertEquals("Mohandar", page.getEntities().get(1).getName().getFirstName());
        Assert.assertTrue(Persistence.getPersistenceUtil().isLoaded(page.getEntities().get(1).getAnnualLeaves()));
        Assert.assertEquals(3, page.getEntities().get(1).getAnnualLeaves().size());
        
        Assert.assertEquals("Nova", page.getEntities().get(2).getName().getFirstName());
        Assert.assertTrue(Persistence.getPersistenceUtil().isLoaded(page.getEntities().get(2).getAnnualLeaves()));
        Assert.assertEquals(3, page.getEntities().get(2).getAnnualLeaves().size());
        
        Assert.assertEquals("Selendis", page.getEntities().get(3).getName().getFirstName());
        Assert.assertTrue(Persistence.getPersistenceUtil().isLoaded(page.getEntities().get(3).getAnnualLeaves()));
        Assert.assertEquals(1, page.getEntities().get(3).getAnnualLeaves().size());
    }
    
    @Test
    public void testPagingQueryByOracleDistinctRank() {
        /*
         * For oracle: Do paging query on database-level
         * For hsqldb: Has to do paging query on memory-level
         */
        Page<Employee> page = 
                this.employeeRepository.getEmployees(
                        null, 
                        1, 
                        4,
                        Employee__.begin().annualLeaves().end(),
                        Employee__.preOrderBy().name().firstName().asc(),
                        Employee__.preOrderBy().annualLeaves().startTime().desc(),
                        Employee__.preOrderBy().annualLeaves().endTime().desc()
                );
        
        
        /*
         * Two SQLs
         * SQL[0]: generated by org.babyfish.persistence.XQuery.getUnlimitedCount()
         * SQL[1]: generated by javax.persistence.Query.getResultList()
         */
        Assert.assertEquals(2, this.preparedSqlList.size());
        /*
         * The left collection join for "employee.annualLeves" is ignored by the 
         * "org.babyfish.persistence.XQuery.getUnlimitedCount()", because
         * (1) The join type is left join(org.babyfish.model.jpa.path.GetterType.OPTINAL)
         * (2) The join is not used by any expression of where
         * (3) The query type is org.babyfish.persistence.QueryType.DISTINCT so that collection join can be ignored
         */
        Assert.assertEquals(
                "select count(employee0_.EMPLOYEE_ID) "
                + "from EMPLOYEE employee0_", 
                this.preparedSqlList.get(0)
        );
        if (this.isOracle()) {
            /*
             * Unfortunately, NOT only the columns of the root entity(Employee) is used by the 
             * "order by" clause, babyfish-hibernate must use the user defined analytic function 
             * "DISTINCT_RANK(ROWID)" to apply it.
             *      
             * "DISTINCT_RANK(ROWID)" is not built-in analytic function, it's an 
             * user-defined analytic function which is defined by babyfish. You can
             * install it into Oracle database by two ways: automatically or manually.
             * (Please see the document to know more).
             *      
             * In this demo, we choose to install the user-defined analytic function 
             * "DISTINCT_RANK(ROWID)" automatically, by the babyfish-hibernate property
             * "babyfish.hibernate.create_oracle_distinct_rank".
             * Please see "src/main/resources/data-access-layer.xml" to know more.
             */
            Assert.assertEquals(
                    "select "
                    +     "* "
                    + "from ("
                    +     "select "
                    +         "<...many columns of employee0_...>, "
                    +         "<...many columns of annualleav1_...>, "
                    +         "distinct_rank(employee0_.rowid) over("
                    +             "order by "
                    +                 "employee0_.FIRST_NAME asc, "
                    +                 "annualleav1_.START_TIME desc, "
                    +                 "annualleav1_.END_TIME desc"
                    +         ") distinct_rank____ "
                    +     "from EMPLOYEE employee0_ "
                    +     "left outer join ANNUAL_LEAVE annualleav1_ "
                    +         "on employee0_.EMPLOYEE_ID=annualleav1_.EMPLOYEE_ID "
                    + ") "
                    + "where "
                    +     "distinct_rank____ <= ? "
                    + "and "
                    +     "distinct_rank____ > ?", 
                    this.preparedSqlList.get(1)
            );
        } else {
            /*
             * Because this paging query contains collection fetch, hsqldb has to apply
             * it on memory level, query all the result at first, then only return the 5th, 
             * 6th, 7th and 8th rows.
             *     
             * Be different with hibernate, acquiescently, memory-level paging query is 
             * disabled in babyfish-hibernate, so you must use the babyfish-hibernate property
             * "babyfish.hibernate.enable_limit_in_memory" to enable it.
             * Please see "src/main/resources/data-access-layer.xml" to know more.
             */
            Assert.assertEquals(
                    "select "
                    +     "<...many columns of employee0_...>, "
                    +     "<...many columns of annualleav1_...> "
                    + "from EMPLOYEE employee0_ "
                    + "left outer join ANNUAL_LEAVE annualleav1_ "
                    +     "on employee0_.EMPLOYEE_ID=annualleav1_.EMPLOYEE_ID "
                    + "order by "
                    +     "employee0_.FIRST_NAME asc, "
                    +     "annualleav1_.START_TIME desc, "
                    +     "annualleav1_.END_TIME desc", 
                    this.preparedSqlList.get(1)
            );
        }
        
        
        Assert.assertEquals(1, page.getActualPageIndex());
        Assert.assertEquals(12, page.getTotalRowCount());
        Assert.assertEquals(3, page.getTotalPageCount());
        Assert.assertEquals(
                "[ "
                +   "{ "
                +     "name: { "
                +       "firstName: Matt, "
                +       "lastName: Horner "
                +     "}, "
                +     "gender: MALE, "
                +     "birthday: 1990-07-14, "
                +     "description: @UnloadedClob, "
                +     "image: @UnloadedBlob, "
                +     "department: @UnloadedReference, "
                +     "annualLeaves: [ "
                +       "{ "
                +         "startTime: 2015-11-15 09:00, "
                +         "endTime: 2015-11-15 11:00, "
                +         "state: PENDING "
                +       "}, "
                +       "{ "
                +         "startTime: 2015-10-31 09:00, "
                +         "endTime: 2015-10-31 18:00, "
                +         "state: PENDING "
                +       "} "
                +     "] "
                +   "}, "
                +   "{ "
                +     "name: { "
                +       "firstName: Mohandar, "
                +       "lastName: null "
                +     "}, "
                +     "gender: MALE, "
                +     "birthday: 1423-02-17, "
                +     "description: @UnloadedClob, "
                +     "image: @UnloadedBlob, "
                +     "department: @UnloadedReference, "
                +     "annualLeaves: [ "
                +       "{ "
                +         "startTime: 2015-01-07 09:00, "
                +         "endTime: 2015-02-28 18:00, "
                +         "state: REJECTED "
                +       "}, "
                +       "{ "
                +         "startTime: 2015-01-07 09:00, "
                +         "endTime: 2015-02-20 18:00, "
                +         "state: REJECTED "
                +       "}, "
                +       "{ "
                +         "startTime: 2015-01-07 09:00, "
                +         "endTime: 2015-02-07 18:00, "
                +         "state: APPROVED "
                +       "} "
                +     "] "
                +   "}, "
                +   "{ "
                +     "name: { "
                +       "firstName: Nova, "
                +       "lastName: Terra "
                +     "}, "
                +     "gender: FEMALE, "
                +     "birthday: 1993-05-04, "
                +     "description: @UnloadedClob, "
                +     "image: @UnloadedBlob, "
                +     "department: @UnloadedReference, "
                +     "annualLeaves: [ "
                +       "{ "
                +         "startTime: 2015-09-15 14:00, "
                +         "endTime: 2015-09-15 18:00, "
                +         "state: PENDING "
                +       "}, "
                +       "{ "
                +         "startTime: 2015-07-10 09:00, "
                +         "endTime: 2015-07-12 18:00, "
                +         "state: APPROVED "
                +       "}, "
                +       "{ "
                +         "startTime: 2015-07-01 09:00, "
                +         "endTime: 2015-07-04 18:00, "
                +         "state: APPROVED "
                +       "} "
                +     "] "
                +   "}, "
                +   "{ "
                +     "name: { "
                +       "firstName: Selendis, "
                +       "lastName: null "
                +     "}, "
                +     "gender: FEMALE, "
                +     "birthday: 1003-12-11, "
                +     "description: @UnloadedClob, "
                +     "image: @UnloadedBlob, "
                +     "department: @UnloadedReference, "
                +     "annualLeaves: [ "
                +       "{ "
                +         "startTime: 2015-08-22 09:00, "
                +         "endTime: 2015-08-23 18:00, "
                +         "state: APPROVED "
                +       "} "
                +     "] "
                +   "} "
                + "]", 
                getEmployeesString(page.getEntities())
        );
    }
    
    @Test
    public void testPagingQueryByOracleDistinctRankAndDistinctRowCount() {
        
        /*
         * For oracle: Do paging query on database-level
         * For hsqldb: Has to do paging query on memory-level
         */
        Page<Employee> page = 
                this.employeeRepository.getEmployees(
                        null, 
                        1, 
                        4,
                        Employee__.begin().annualLeaves(GetterType.REQUIRED).end(), // (1) GetterType.REQUIRED means "Inner Join".
                        Employee__.preOrderBy().name().firstName().asc(),
                        Employee__.preOrderBy().annualLeaves().startTime().desc(), // (2) Not inner join, but after merge, it's inner join
                        Employee__.preOrderBy().annualLeaves().endTime().desc() // (3) Not inner join, but after merge, it's inner join
                );
        
        
        /*
         * Two SQLs
         * SQL[0]: generated by org.babyfish.persistence.XQuery.getUnlimitedCount()
         * SQL[1]: generated by javax.persistence.Query.getResultList()
         */
        Assert.assertEquals(2, this.preparedSqlList.size());
        /*
         * Unfortuantely, Though query mode is distinct, but the join type is "INNER" 
         * so that the optimizer can not remove it automatically, babyfish-hibernate 
         * has to keep the join and use "count(distinct ...)"
         */
        Assert.assertEquals(
                "select count(distinct employee0_.EMPLOYEE_ID) "
                + "from EMPLOYEE employee0_ "
                + "inner join ANNUAL_LEAVE annualleav1_ "
                +     "on employee0_.EMPLOYEE_ID=annualleav1_.EMPLOYEE_ID", 
                this.preparedSqlList.get(0)
        );
        if (this.isOracle()) {
            /*
             * Unfortunately, NOT only the columns of the root entity(Employee) is used by the 
             * "order by" clause, babyfish-hibernate must use the user defined analytic function 
             * "DISTINCT_RANK(ROWID)" to apply it.
             *      
             * "DISTINCT_RANK(ROWID)" is not built-in analytic function, it's an 
             * user-defined analytic function which is defined by babyfish. You can
             * install it into Oracle database by two ways: automatically or manually.
             * (Please see the document to know more).
             *      
             * In this demo, we choose to install the user-defined analytic function 
             * "DISTINCT_RANK(ROWID)" automatically, by the babyfish-hibernate property
             * "babyfish.hibernate.create_oracle_distinct_rank".
             * Please see "src/main/resources/data-access-layer.xml" to know more.
             */
            Assert.assertEquals(
                    "select "
                    +     "* "
                    + "from ("
                    +     "select "
                    +         "<...many columns of employee0_...>, "
                    +         "<...many columns of annualleav1_...>, "
                    +         "distinct_rank(employee0_.rowid) over("
                    +             "order by "
                    +                 "employee0_.FIRST_NAME asc, "
                    +                 "annualleav1_.START_TIME desc, "
                    +                 "annualleav1_.END_TIME desc"
                    +         ") distinct_rank____ "
                    +     "from EMPLOYEE employee0_ "
                    +     "inner join ANNUAL_LEAVE annualleav1_ "
                    +         "on employee0_.EMPLOYEE_ID=annualleav1_.EMPLOYEE_ID "
                    + ") "
                    + "where "
                    +     "distinct_rank____ <= ? "
                    + "and "
                    +     "distinct_rank____ > ?", 
                    this.preparedSqlList.get(1)
            );
        } else {
            /*
             * Because this paging query contains collection fetch, hsqldb has to apply
             * it on memory level, query all the result at first, then only return the 5th, 
             * 6th, 7th and 8th rows.
             *     
             * Be different with hibernate, acquiescently, memory-level paging query is 
             * disabled in babyfish-hibernate, so you must use the babyfish-hibernate property
             * "babyfish.hibernate.enable_limit_in_memory" to enable it.
             * Please see "src/main/resources/data-access-layer.xml" to know more.
             */
            Assert.assertEquals(
                    "select "
                    +     "<...many columns of employee0_...>, "
                    +     "<...many columns of annualleav1_...> "
                    + "from EMPLOYEE employee0_ "
                    + "inner join ANNUAL_LEAVE annualleav1_ "
                    +     "on employee0_.EMPLOYEE_ID=annualleav1_.EMPLOYEE_ID "
                    + "order by "
                    +     "employee0_.FIRST_NAME asc, "
                    +     "annualleav1_.START_TIME desc, "
                    +     "annualleav1_.END_TIME desc", 
                    this.preparedSqlList.get(1)
            );
        }
    
        
        Assert.assertEquals(1, page.getActualPageIndex());
        
        /*
         * Then employee "karass" does not have annualLeaves, 
         * so it's excluded by the inner join.
         * The final totalRowCount is 11, not 12
         */
        Assert.assertEquals(11, page.getTotalRowCount());
        
        Assert.assertEquals(3, page.getTotalPageCount());
        /*
         * Be different with the result of "testPagingQueryByOracleDistinctRank",
         * the employee "Matt Horner" is not in the result but "Tassadar" is.
         */
        Assert.assertEquals(
                "[ "
                +   "{ "
                +     "name: { "
                +       "firstName: Mohandar, "
                +       "lastName: null "
                +     "}, "
                +     "gender: MALE, "
                +     "birthday: 1423-02-17, "
                +     "description: @UnloadedClob, "
                +     "image: @UnloadedBlob, "
                +     "department: @UnloadedReference, "
                +     "annualLeaves: [ "
                +       "{ "
                +         "startTime: 2015-01-07 09:00, "
                +         "endTime: 2015-02-28 18:00, "
                +         "state: REJECTED "
                +       "}, "
                +       "{ "
                +         "startTime: 2015-01-07 09:00, "
                +         "endTime: 2015-02-20 18:00, "
                +         "state: REJECTED "
                +       "}, "
                +       "{ "
                +         "startTime: 2015-01-07 09:00, "
                +         "endTime: 2015-02-07 18:00, "
                +         "state: APPROVED "
                +       "} "
                +     "] "
                +   "}, "
                +   "{ "
                +     "name: { "
                +       "firstName: Nova, "
                +       "lastName: Terra "
                +     "}, "
                +     "gender: FEMALE, "
                +     "birthday: 1993-05-04, "
                +     "description: @UnloadedClob, "
                +     "image: @UnloadedBlob, "
                +     "department: @UnloadedReference, "
                +     "annualLeaves: [ "
                +       "{ "
                +         "startTime: 2015-09-15 14:00, "
                +         "endTime: 2015-09-15 18:00, "
                +         "state: PENDING "
                +       "}, "
                +       "{ "
                +         "startTime: 2015-07-10 09:00, "
                +         "endTime: 2015-07-12 18:00, "
                +         "state: APPROVED "
                +       "}, "
                +       "{ "
                +         "startTime: 2015-07-01 09:00, "
                +         "endTime: 2015-07-04 18:00, "
                +         "state: APPROVED "
                +       "} "
                +     "] "
                +   "}, "
                +   "{ "
                +     "name: { "
                +       "firstName: Selendis, "
                +       "lastName: null "
                +     "}, "
                +     "gender: FEMALE, "
                +     "birthday: 1003-12-11, "
                +     "description: @UnloadedClob, "
                +     "image: @UnloadedBlob, "
                +     "department: @UnloadedReference, "
                +     "annualLeaves: [ "
                +       "{ "
                +         "startTime: 2015-08-22 09:00, "
                +         "endTime: 2015-08-23 18:00, "
                +         "state: APPROVED "
                +       "} "
                +     "] "
                +   "}, "
                +   "{ "
                +     "name: { "
                +       "firstName: Tassadar, "
                +       "lastName: null "
                +     "}, "
                +     "gender: MALE, "
                +     "birthday: 0756-08-24, "
                +     "description: @UnloadedClob, "
                +     "image: @UnloadedBlob, "
                +     "department: @UnloadedReference, "
                +     "annualLeaves: [ "
                +       "{ "
                +         "startTime: 2015-09-05 19:00, "
                +         "endTime: 2015-09-06 18:00, "
                +         "state: PENDING "
                +       "}, "
                +       "{ "
                +         "startTime: 2015-04-21 09:00, "
                +         "endTime: 2015-04-24 18:00, "
                +         "state: APPROVED "
                +       "} "
                +     "] "
                +   "} "
                + "]", 
                getEmployeesString(page.getEntities())
        );
    }
}
