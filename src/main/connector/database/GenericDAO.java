/*
 * Copyright 2017 EnergySys Limited. All Rights Reserved.
 * 
 * This software is the proprietary information of EnergySys Limited.
 * Use is subject to licence terms.
 * This software is not designed or supplied to be used in circumstances where
 * personal injury, death or loss of or damage to property could result from any
 * defect in the software.
 * In no event shall the developers or owners be liable for personal injury,
 * death or loss or damage to property, loss of business, revenue, profits, use,
 * data or other economic advantage or for any indirect, punitive, special,
 * incidental, consequential or exemplary loss or damage resulting from the use
 * of the software or documentation.
 * Developer and owner make no warranties, representations or undertakings of
 * any nature in relation to the software and documentation.
 */
package com.energysys.connector.database;

import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaOrder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Generic DAO class. On construction, creates a session and transaction. Exceptions during operations of this object
 * result in a rollback.
 *
 * Note that this class is AutoCloseable so can be constructed within a try declaration.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
public class GenericDAO implements AutoCloseable
{
  /**
   * Enumeration for order by.
   */
  public enum ORDERBY
  {
    /** Ascending. **/
    ASC,
    /** Descending. **/
    DESC
  }
  private static final Logger LOG = Logger.getLogger(GenericDAO.class.getName());

  private static SessionFactory soleSessionFactory;

  private final Session theSession;

  private Boolean isUseCurrentSession;

  /**
   * Default Constructor.
   */
  public GenericDAO()
  {
    this(false);
  }

  /**
   * Constructor setting whether this DAO should use the current session or not.
   *
   * @param isUseCurrentSession if true
   */
  public GenericDAO(Boolean isUseCurrentSession)
  {
    this.isUseCurrentSession = isUseCurrentSession;
    if (isUseCurrentSession)
    {
      theSession = soleSessionFactory.getCurrentSession();
    }
    else
    {
      theSession = soleSessionFactory.openSession();
    }
    theSession.beginTransaction();
  }

  public static SessionFactory getSoleSessionFactory()
  {
    return soleSessionFactory;
  }

  /**
   * Initialises the session factory.
   *
   * @param aSessionFactory the session factory
   */
  public static void initSessionFactory(SessionFactory aSessionFactory)
  {
    soleSessionFactory = aSessionFactory;
  }

  /**
   * Counts number of rows of a given class.
   *
   * @param <T>
   * @param aClass the class
   * @return count
   */
  @SuppressWarnings("unchecked")
  public <T> Long countAll(Class<T> aClass)
  {
    CriteriaBuilder criteriaBuilder = theSession.getCriteriaBuilder();

    CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
    Root<T> myRoot = criteriaQuery.from(aClass);
    criteriaQuery.select(criteriaBuilder.count(myRoot));
    return theSession.createQuery(criteriaQuery).getSingleResult();
  }

  /**
   * Counds number of rows of a given class that match a where clause.
   * @param aClass
   * @param whereClause
   * @return the count
   * @param <T>
   */
  @SuppressWarnings("unchecked")
  public <T> Long countAll(Class<T> aClass, Map<String, Object> whereClause)
  {
    CriteriaBuilder criteriaBuilder = theSession.getCriteriaBuilder();

    CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
    Root<T> myRoot = criteriaQuery.from(aClass);


    List<Predicate> myPredicates = new ArrayList<>();
    whereClause.forEach(
            (aColumnName, aValue) ->
                    myPredicates.add(criteriaBuilder.equal(myRoot.get(aColumnName), aValue)));

    criteriaQuery.where(myPredicates.toArray(Predicate[] ::new));

    criteriaQuery.select(criteriaBuilder.count(myRoot));
    return theSession.createQuery(criteriaQuery).getSingleResult();
  }

  /**
   * Deletes the given object from the database.
   *
   * @param anObject the object
   */
  @SuppressWarnings("checkstyle:illegalcatch")
  public void delete(Object anObject)
  {
    try
    {
      theSession.delete(anObject);
    }
    catch (Exception ex)
    {
      theSession.getTransaction().rollback();
      throw ex;
    }
  }

  /**
   * Deletes all entries of the specified type. Basically truncates the table.
   *
   * @param <T> the type
   * @param aClass the entity class
   */
  public <T> void deleteAll(Class<T> aClass)
  {
    String myTableName = aClass.getAnnotation(Table.class).name();
    Query myTruncateQuery = theSession.createQuery("delete from " + aClass.getSimpleName());
    myTruncateQuery.executeUpdate();

  }

  /**
   * Finds the object of a given class that matches a given id.
   *
   * @param <T> type to find
   * @param aClass class to find
   * @param anId id to find
   * @return the object
   */
  public <T> T findById(Class<T> aClass, Serializable anId)
  {
    return (T) theSession.get(aClass, anId);
  }


  /**
   * Lists objects given a where clause.
   * @param aClass
   * @param whereClause
   * @return list of objects
   * @param <T>
   */
  public <T> List<T> list(Class<T> aClass, Map<String, Object> whereClause)
  {
    CriteriaBuilder myCB = theSession.getCriteriaBuilder();
    CriteriaQuery<T> myQuery = myCB.createQuery(aClass);
    Root<T> myRoot = myQuery.from(aClass);

    List<Predicate> myPredicates = new ArrayList<>();
    whereClause.forEach((aColumnName, aValue) -> myPredicates.add(myCB.equal(myRoot.get(aColumnName), aValue)));
    myQuery.where(myPredicates.toArray(Predicate[] ::new));

    return list(myQuery, myRoot);
  }

  /**
   * Gets the session factory.
   *
   * @return the session factory.
   */
  public SessionFactory getSessionFactory()
  {
    return soleSessionFactory;
  }

  /**
   * Lists all objects of a given type in the database.
   *
   * @param <T> the type
   * @param aClass the class
   * @return list
   */
  public <T> List<T> list(Class<T> aClass)
  {
    HibernateCriteriaBuilder criteriaBuilder = theSession.getCriteriaBuilder();
    CriteriaQuery myCriteria = criteriaBuilder.createQuery(aClass);
    Root<T> myRoot = myCriteria.from(aClass);
    return list(myCriteria, myRoot);
  }

  /**
   * Lists all objects of a given type in the database.
   *
   * @param <T> the type
   * @param aClass the class
   * @param anOrderByColumn the ordering column
   * @param order the order
   * @return list
   */
  public <T> List<T> list(Class<T> aClass, String anOrderByColumn, ORDERBY order)
  {
    HibernateCriteriaBuilder criteriaBuilder = theSession.getCriteriaBuilder();
    CriteriaQuery myCriteria = criteriaBuilder.createQuery(aClass);
    Root<T> myRoot = myCriteria.from(aClass);

    switch (order)
    {
      case ASC:
        myCriteria.orderBy(criteriaBuilder.asc(myRoot.get(anOrderByColumn)));
        break;

      case DESC:
      default:
        myCriteria.orderBy(criteriaBuilder.desc(myRoot.get(anOrderByColumn)));
        break;
    }

    return list(myCriteria, myRoot);
  }


  private <T> List<T> list(CriteriaQuery aCriteria, Root<T> aRoot)
  {
    aCriteria.select(aRoot);
    return theSession.createQuery(aCriteria).getResultList();
  }

  /**
   * Lists objects starting and ending at the given rows. Used for pagination
   *
   * @param <T> the type
   * @param aClass the class
   * @param aStartRow the start row
   * @param aPageSize the end row
   * @param anOrderByColumn the ordering
   * @param aDirection the order direction
   * @return list
   */
  public <T> List<T> list(Class<T> aClass, int aStartRow, int aPageSize, String anOrderByColumn, ORDERBY aDirection)
  {
    HibernateCriteriaBuilder criteriaBuilder = theSession.getCriteriaBuilder();
    CriteriaQuery myCriteria = criteriaBuilder.createQuery(aClass);
    Root<T> myRoot = myCriteria.from(aClass);

    JpaOrder myOrder = null;
    switch (aDirection)
    {
      case ASC:
        myOrder = theSession.getCriteriaBuilder().asc(myRoot.get(anOrderByColumn));
        break;
      case DESC:
      default:
        myOrder = theSession.getCriteriaBuilder().desc(myRoot.get(anOrderByColumn));
        break;
    }
    myCriteria.select(myRoot).orderBy(myOrder);

    TypedQuery<T> query = theSession.createQuery(myCriteria);
    return query.setFirstResult(aStartRow).setMaxResults(aPageSize).getResultList();
  }

  /**
   * Saves the given object to the database.
   *
   * @param anObject object
   * @return the id of the saved Object
   */
  @SuppressWarnings("checkstyle:illegalcatch")
  public Object save(Object anObject)
  {
    try
    {
      return theSession.save(anObject);
    }
    catch (Exception ex)
    {
      theSession.getTransaction().rollback();
      throw ex;
    }
  }

  /**
   * Updates the given object in the database.
   *
   * @param anObject object
   */
  @SuppressWarnings("checkstyle:illegalcatch")
  public void update(Object anObject)
  {
    try
    {
      theSession.update(anObject);
    }
    catch (Exception ex)
    {
      theSession.getTransaction().rollback();
      throw ex;
    }
  }

  /**
   * Closes the transaction and session.
   */
  @SuppressWarnings(
  {
    "checkstyle:illegalcatch", "UseSpecificCatch"
  })
  @Override
  public void close()
  {
    try
    {
      if (theSession.getTransaction().isActive())
      {
        theSession.getTransaction().commit();

        if (theSession.isOpen() && !isUseCurrentSession)
        {
          theSession.close();
        }
      }
    }
    catch (Exception ex)
    {
      if (theSession.getTransaction().isActive())
      {
        theSession.getTransaction().rollback();
      }
      if (theSession.isOpen() && !isUseCurrentSession)
      {
        theSession.close();
      }
      throw ex;
    }
  }

  /**
   * Gets a Query from the current session.
   *
   * @param aQuery a query string
   * @return Query
   */
  public Query getQuery(String aQuery)
  {
    return theSession.createQuery(aQuery);
  }

  /**
   * Merges an object.
   *
   * @param anObject an object to merge
   * @return the merged object
   */
  public Object merge(Object anObject)
  {
    return theSession.merge(anObject);
  }

}
