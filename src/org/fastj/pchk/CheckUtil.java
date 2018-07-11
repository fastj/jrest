package org.fastj.pchk;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fastj.pchk.annotation.StringEnum;
import org.fastj.pchk.annotation.Check;
import org.fastj.pchk.annotation.DoubleEnum;
import org.fastj.pchk.annotation.DoubleRange;
import org.fastj.pchk.annotation.IntEnum;
import org.fastj.pchk.annotation.Length;
import org.fastj.pchk.annotation.NotNull;
import org.fastj.pchk.annotation.Range;
import org.fastj.pchk.annotation.Size;

public class CheckUtil {
	
	static HashMap<Class<?>, PChecker> chkmap = new HashMap<>();
	static HashMap<Class<?>, VOChkCfg> chkcfgmap = new HashMap<>();
	
	static {
		chkmap.put(StringEnum.class, new INChecker());
		chkmap.put(Length.class, new LengthChecker());
		chkmap.put(NotNull.class, new NotNullChecker());
		chkmap.put(Size.class, new SizeChecker());
		chkmap.put(IntEnum.class, new EnumChecker());
		chkmap.put(StringEnum.class, new EnumChecker());
		chkmap.put(DoubleEnum.class, new EnumChecker());
		chkmap.put(Range.class, new RangeChecker());
		chkmap.put(DoubleRange.class, new RangeChecker());
		
	}
	
	public static List<String> check(ChkNode ... cns) {
		
		List<String> error = new ArrayList<>();
		
		if (cns == null || cns.length == 0) {
			return error;
		}
		
		for (ChkNode node : cns) {
			for (Annotation ao : node.chklist) {
				try {
					
					PChecker chk = chkmap.get(ao.annotationType());
					if (chk == null) {
						continue;
					}
					
					String emsg = chk.check(node.value, ao);
					if (emsg != null) {
						error.add(emsg);
					}
				} catch (Throwable e) {
					error.add("Check Exception: " + e.getMessage());
				}
			}
			if (node.value != null && !node.value.getClass().isPrimitive() && node.value.getClass() != String.class) {
				List<String> errs = checkVo(node.value);
				error.addAll(errs);
			}
			
		}
		
		return error;
		
	}
	
	public static List<String> checkVo(Object vo) {
		
		Class<?> clazz = vo.getClass();
		VOChkCfg ccfg = chkCfg(clazz);
		List<String> errors = new ArrayList<>();
		
		for (Map.Entry<String, CheckUtil.ChkNode> fd : ccfg.fieldChks.entrySet()) {
			
			ChkNode cn = fd.getValue().copy(null);
			try {
				Field f = clazz.getDeclaredField(cn.key);
				f.setAccessible(true);
				Object v = f.get(vo);
				cn.value = v;
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			List<String> errs = check(cn);
			errors.addAll(errs);
		}
		
		return errors;
	}
	
	public static ChkNode chkNode(Field f) {
		return chkNode(f.getAnnotations());
	}
	
	public static ChkNode chkNode(Parameter p) {
		return chkNode(p.getAnnotations());
	}
	
	public static ChkNode chkNode(Annotation[] aos) {
		
		ChkNode node = new ChkNode();
		if (aos != null) {
			for (Annotation a : aos) {
				if (a.annotationType().getName().contains(".pchk.annotation.") 
						&& !a.annotationType().getName().endsWith("pchk.annotation.Check")) {
					node.chklist.add(a);
				}
			}
		}
		
		return node.chklist.isEmpty() ? null : node;
	}
	
	public static VOChkCfg chkCfg(Class<?> voc) {
		
		VOChkCfg cfg = chkcfgmap.get(voc);
		if (cfg != null) return cfg;
		
		cfg = new VOChkCfg();
		
		Field[] fds = voc.getDeclaredFields();
		
		for (Field f : fds) {
			ChkNode cn = chkNode(f);
			if (cn != null) {
				cn.key = f.getName();
				cfg.fieldChks.put(f.getName(), cn);
			}
			Check c = f.getAnnotation(Check.class);
			if (c != null) {
				Class<?> fclazz = f.getType();
				//TODO
				Type ftype = f.getGenericType();
				Class<?> pedtype = null;
				if (ftype instanceof ParameterizedType) {
					ParameterizedType pt = (ParameterizedType) ftype;
					pt.getActualTypeArguments();
				}
				if (fclazz.isPrimitive() || fclazz == String.class || fclazz.isEnum()
						|| Collection.class.isAssignableFrom(fclazz) 
						|| Map.class.isAssignableFrom(fclazz)) {
					continue;
				}
				VOChkCfg subcfg = chkCfg(fclazz);
				cfg.deepChks.put(f.getName(), subcfg);
			}
		}
		
		return cfg;
	}
	
	public static class ChkNode {
		
		public String key;
		
		public Object value;
		
		public List<Annotation> chklist = new ArrayList<>();
		
		public ChkNode copy(Object rvalue) {
			ChkNode rcn = new ChkNode();
			rcn.key = key;
			rcn.value = rvalue;
			rcn.chklist.addAll(chklist);
			return rcn;
		}
		
	}
	
	
	
	public static class VOChkCfg {
		
		public String fieldName = null;
		
		public HashMap<String, ChkNode> fieldChks = new HashMap<>(7);
		
		public HashMap<String, VOChkCfg> deepChks = new HashMap<>(3);
		
	}
	
	public static void main(String[] args) {
		
		Class a = List.class;
		Class b = Collection.class;
		System.out.println(Map.class.isAssignableFrom(HashMap.class));
		
		
		
		
	}
	
}
