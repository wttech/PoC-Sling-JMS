package com.cognifide.jms.session;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

import com.cognifide.jms.api.session.ClassLoaderProvider;

@Component(immediate = true, metatype = false)
@Service(value = ClassLoaderRegistry.class)
public class ClassLoaderRegistry {
	@Reference(referenceInterface = ClassLoaderProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	private Set<ClassLoaderProvider> providers = new HashSet<ClassLoaderProvider>();

	private volatile ClassLoader loader = Thread.currentThread().getContextClassLoader();

	public ClassLoader getClassLoader() {
		return loader;
	}

	public void bindProviders(ClassLoaderProvider provider) {
		providers.add(provider);
		loader = createClassLoader(providers);
	}

	public void unbindProviders(ClassLoaderProvider provider) {
		providers.remove(provider);
		loader = createClassLoader(providers);
	}

	private ClassLoader createClassLoader(Set<ClassLoaderProvider> providers) {
		return new CombinedClassLoader(Thread.class.getClassLoader(), providers);
	}

	private static final class CombinedClassLoader extends ClassLoader {
		private final List<ClassLoader> classLoaders;

		public CombinedClassLoader(ClassLoader parent, Set<ClassLoaderProvider> providers) {
			super(parent);
			classLoaders = new ArrayList<ClassLoader>();
			for (ClassLoaderProvider p : providers) {
				classLoaders.addAll(p.getClassLoaders());
			}
		}

		public Class<?> loadClass(String name) throws ClassNotFoundException {
			try {
				return super.loadClass(name);
			} catch (ClassNotFoundException e) {
			}
			for (ClassLoader cl : classLoaders) {
				try {
					return cl.loadClass(name);
				} catch (ClassNotFoundException e) {
				}
			}
			throw new ClassNotFoundException();
		}
	}

}
