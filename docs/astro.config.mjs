// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

// https://astro.build/config
export default defineConfig({
	integrations: [
		starlight({
			title: 'Levtus',
			social: [{ icon: 'github', label: 'GitHub', href: 'https://github.com/Bernardusz/Levtus' }],
			sidebar: [
				{
					label: 'Guides',
					items: [
						// Each item here is one entry in the navigation menu.
						{ label: 'Example Guide', slug: 'guides/example' },
					],
				},
				{
					label: 'Reference',
					items: [{ autogenerate: { directory: 'reference' } }],
				},
			],
			logo: {
				light: "./src/assets/Levtus_Logo-Light_Mode.svg",
				dark: "./src/assets/Levtus_Logo-Dark_Mode.svg",
				replacesTitle: true,
			},
			customCss: [
				'./src/styles/style.css',
			],
		}),
	],
});
