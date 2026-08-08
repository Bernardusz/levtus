// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

// https://astro.build/config
export default defineConfig({
	integrations: [
		starlight({
			title: 'Levtus',
			favicon: './src/assets/Levtus_Logo-Dark_Mode.svg',
			social: [{ icon: 'github', label: 'GitHub', href: 'https://github.com/Bernardusz/Levtus' }],
			sidebar: [
				{
					label: 'Docs',
					items: [
						// Each item here is one entry in the navigation menu.
						{ label: 'Introduction', slug: 'docs' },
						{ label: 'Getting Started', slug: 'docs/getting-started' },
						{
							label: 'API',
							collapsed: true,
							items: [
								{ label: 'Application Creation', slug: 'docs/api/application-creation' },
								{ label: 'Creating a Route', slug: 'docs/api/routing-parameters' },
								{ label: 'Request API', slug: 'docs/api/request-api' },
								{ label: 'Response API', slug: 'docs/api/response-api' },
								{ label: 'Levtus Context API', slug: 'docs/api/levtus-context-api' }
							]
						}
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
