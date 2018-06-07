package org.fastj.db;

import java.util.Collection;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

@SuppressWarnings("deprecation")
public class BO {

	public static final int INVALID = 0;
	public static final int SQL = 1;
	public static final int HQL = 2;
	public static final int DELETE_OBJ = 3;
	public static final int SAVE_OBJ = 4;
	public static final int UPDATE_OBJ = 5;

	private int type = INVALID;
	private Object value = null;
	private Object[] parameters = null;

	public void exec(Session s) {
		if (type == INVALID || value == null) {
			return;
		}

		switch (type) {
		case SQL:
			NativeQuery<?> nq = buildSql(s, Void.class, (String) value, parameters);
			nq.executeUpdate();
			break;
		case HQL:
			Query<?> q = buildHql(s, Void.class, (String) value, parameters);
			q.executeUpdate();
			break;
		case DELETE_OBJ:
			s.delete(value);
			break;
		case SAVE_OBJ:
			s.save(value);
			break;
		case UPDATE_OBJ:
			s.update(value);
			break;
		default:
			break;
		}
	}

	public static BO sql(String sql) {
		BO bo = new BO();
		bo.type = SQL;
		bo.value = sql;
		return bo;
	}

	public static BO hsql(String hsql) {
		BO bo = new BO();
		bo.type = HQL;
		bo.value = hsql;
		return bo;
	}

	public static BO delete(Object obj) {
		BO bo = new BO();
		bo.type = DELETE_OBJ;
		bo.value = obj;
		return bo;
	}

	public static BO save(Object obj) {
		BO bo = new BO();
		bo.type = SAVE_OBJ;
		bo.value = obj;
		return bo;
	}

	public static BO update(Object obj) {
		BO bo = new BO();
		bo.type = UPDATE_OBJ;
		bo.value = obj;
		return bo;
	}

	public int type() {
		return type;
	}

	public Object value() {
		return value;
	}

	public Object[] parameters() {
		return parameters;
	}

	public BO parameters(Object... params) {
		parameters = params;
		return this;
	}

	public static NativeQuery<?> buildSql(Session s, Class<?> clazz, String sql, Object[] parameters) {
		NativeQuery<?> nq = (NativeQuery<?>) s.createNativeQuery(sql);
		buildParams(nq, parameters);
		return nq;
	}

	public static <T> Query<?> buildHql(Session s, Class<T> clazz, String hql, Object[] parameters) {
		Query<?> q = null;
		if (hql.toLowerCase().startsWith("from")) {
			q = s.createQuery((String) hql, clazz);
		} else {
			q = s.createQuery((String) hql);
			q.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP);
		}

		buildParams(q, parameters);
		return q;
	}

	private static void buildParams(Query<?> q, Object[] parameters) {
		int lidx = 1;
		int oidx = 1;
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i] instanceof Collection<?>) {
				q.setParameterList("plist" + (lidx++), (Collection<?>) parameters[i]);
			} else if (parameters[i] instanceof Object[]) {
				q.setParameterList("plist" + (lidx++), (Object[]) parameters[i]);
			} else {
				q.setParameter(oidx++, parameters[i]);
			}
		}
	}
}
