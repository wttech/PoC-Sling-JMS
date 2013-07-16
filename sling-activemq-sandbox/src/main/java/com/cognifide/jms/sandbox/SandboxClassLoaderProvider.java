package com.cognifide.jms.sandbox;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import com.cognifide.jms.api.session.ClassLoaderProvider;

@Component(immediate = true, metatype = false)
@Service
public class SandboxClassLoaderProvider implements ClassLoaderProvider {

	@Override
	public List<ClassLoader> getClassLoaders() {
		return Arrays.asList(Value.class.getClassLoader());
	}

}
