package org.fastj.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.fastj.log.LogUtil;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.NativeQuery;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("deprecation")
public class DaoService {

	private final String key;

	private DaoService(String key) {
		this.key = key;
	}

	public static DaoService by(String key) {
		return new DaoService(key);
	}

	public <T> List<T> list(Class<T> clazz, Object... filters) {
		return pageList(clazz, -1, -1, filters);
	}

	public <T> List<T> pageList(Class<T> clazz, int start, int size, Object... filters) {

		Session s = get();

		Transaction tx = s.beginTransaction();

		try {
			Criteria c = s.createCriteria(clazz);
			buildCriteria(c, filters);
			if (start >= 0 || size > 0) {
				c.setFirstResult(start).setMaxResults(size);
			}

			List<T> l = c.list();
			tx.commit();
			return l;
		} catch (HibernateException e) {
			tx.rollback();
			LogUtil.error("list fail", e);
		}

		return new ArrayList<>();
	}

	public <T> List<T> listKeys(Class<?> model, String ckey, Object... filters) {
		Session s = get();

		Transaction tx = s.beginTransaction();
		List<T> rlt = new ArrayList<>();
		try {
			Criteria c = s.createCriteria(model);
			buildCriteria(c, filters);
			ProjectionList pl = Projections.projectionList().add(Property.forName(ckey));
			List<?> l = c.setProjection(Projections.distinct(pl)).list();
			tx.commit();

			for (Object item : l) {
				rlt.add((T) item);
			}

			return rlt;
		} catch (HibernateException e) {
			tx.rollback();
			LogUtil.error("list fail", e);
		}

		return rlt;
	}

	public <K> HashMap<K, Integer> countBy(Class<?> model, String ckey, Object... filters) {
		Session s = get();

		Transaction tx = s.beginTransaction();
		HashMap<K, Integer> rlt = new HashMap<>();
		try {
			Criteria c = s.createCriteria(model);
			buildCriteria(c, filters);

			ProjectionList pl = Projections.projectionList().add(Projections.groupProperty(ckey)).add(Projections.rowCount());
			List<?> l = c.setProjection(Projections.distinct(pl)).list();
			tx.commit();

			for (Object item : l) {
				Object[] ipara = (Object[]) item;
				Integer cnt = ((Number) ipara[1]).intValue();
				rlt.put((K) ipara[0], cnt);
			}

			return rlt;
		} catch (HibernateException e) {
			tx.rollback();
			LogUtil.error("list fail", e);
		}

		return rlt;
	}

	public <T> List<T> listSql(String sql, Class<T> clazz, Object... parameters) {
		Session s = get();

		Transaction tx = s.beginTransaction();
		List<T> rlt = new ArrayList<>();
		try {
			NativeQuery<?> q = BO.buildSql(s, clazz, sql, parameters);
			q.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP);
			List<Map<?, ?>> mrlt = (List<Map<?, ?>>) q.list();
			tx.commit();

			ObjectMapper om = new ObjectMapper();
			mrlt.forEach(mo -> {
				T to = om.convertValue(mo, clazz);
				rlt.add(to);
			});

			return rlt;
		} catch (HibernateException e) {
			tx.rollback();
			LogUtil.error("save or update fail", e);
		}

		return rlt;
	}

	public <T> List<T> listHql(String hql, Class<T> clazz, Object... parameters) {
		Session s = get();

		Transaction tx = s.beginTransaction();
		final List<T> rlt = new ArrayList<>();
		try {
			Query<?> q = BO.buildHql(s, clazz, hql, parameters);

			if (hql.toLowerCase().startsWith("from")) {
				rlt.addAll((List<T>) q.list());
				tx.commit();
			} else {
				List<?> mrlt = (List<?>) q.list();
				tx.commit();
				ObjectMapper om = new ObjectMapper();
				mrlt.forEach(mo -> {
					T to = om.convertValue(mo, clazz);
					rlt.add(to);
				});
			}

			return rlt;
		} catch (HibernateException e) {
			tx.rollback();
			LogUtil.error("save or update fail", e);
		}

		return new ArrayList<>();
	}

	public <K, T> Map<K, T> mapBy(Class<T> model, Function<T, K> func, Object... filters) {
		List<T> l = list(model, filters);
		return l.stream().collect(Collectors.toMap(func, Function.identity()));
	}

	public <K, V> Map<K, V> mapBy(Class<?> model, String ckey, String cvalue, Object... filters) {
		Session s = get();

		Transaction tx = s.beginTransaction();
		Map<K, V> rlt = new HashMap<>();
		try {
			Criteria c = s.createCriteria(model);
			buildCriteria(c, filters);
			ProjectionList pl = Projections.projectionList().add(Property.forName(ckey)).add(Property.forName(cvalue));
			List<?> l = c.setProjection(pl).list();
			tx.commit();

			for (Object item : l) {
				Object[] ipara = (Object[]) item;
				rlt.put((K) ipara[0], (V) ipara[1]);
			}

			return rlt;
		} catch (HibernateException e) {
			tx.rollback();
			LogUtil.error("mapBy2 fail", e);
		}

		return rlt;
	}

	public <K, V, T> Map<K, V> mapBy(Class<T> model, Function<T, K> funcK, Function<T, V> funcV, Object... filters) {
		List<T> l = list(model, filters);
		return l.stream().collect(Collectors.toMap(funcK, funcV));
	}

	public boolean save(Object obj) {
		Session s = get();

		Transaction tx = s.beginTransaction();

		try {
			s.save(obj);
			tx.commit();
			return true;
		} catch (HibernateException e) {
			tx.rollback();
			LogUtil.error("save or update fail", e);
		}

		return false;
	}

	public boolean update(Object obj) {
		Session s = get();

		Transaction tx = s.beginTransaction();

		try {
			s.update(obj);
			tx.commit();
			return true;
		} catch (HibernateException e) {
			tx.rollback();
			LogUtil.error("save or update fail", e);
		}

		return false;
	}

	public boolean delete(Object obj) {
		Session s = get();

		Transaction tx = s.beginTransaction();

		try {
			s.delete(obj);
			tx.commit();
			return true;
		} catch (HibernateException e) {
			tx.rollback();
			LogUtil.error("save or update fail", e);
		}

		return false;
	}

	public boolean batch(BO... batchObjs) {

		Session s = get();

		Transaction tx = s.beginTransaction();

		try {
			for (BO bo : batchObjs) {
				bo.exec(s);
			}
			tx.commit();
			return true;
		} catch (HibernateException e) {
			tx.rollback();
			LogUtil.error("save or update fail", e);
		}

		return false;
	}

	private void buildCriteria(Criteria c, Object... filters) {
		if (filters == null || filters.length == 0) {
			return;
		}

		int len = filters.length;
		for (int i = 0; i < len; i++) {
			Object fo = filters[i];
			if (fo instanceof Criterion) {
				c.add((Criterion) fo);
			} else if (fo instanceof Order) {
				c.addOrder((Order) fo);
			} else if (fo instanceof String) {
				Object vo = filters[i + 1];
				i++;
				if (vo instanceof Criterion) {
					c.add((Criterion) vo);
				} else if (fo instanceof Order) {
					c.addOrder((Order) vo);
				} else {
					c.add(Restrictions.eq((String) fo, vo));
				}
			} else {
				LogUtil.error("Filter-set invalid: {} = {}", fo.getClass(), fo);
			}
		}
	}

	public Session get() {
		return HSessionUtils.getSession(key);
	}
}
