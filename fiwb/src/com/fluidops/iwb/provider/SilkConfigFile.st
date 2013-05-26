<Silk>
	<Prefixes>
	$namespaceMappings:{ namespaceMapping |
		<Prefix id="$namespaceMapping.prefix$" namespace="$namespaceMapping.namespace$" />
	}$
	</Prefixes>
	
	<DataSources>
		<DataSource id="local" type="iwb" />
	</DataSources>
	
	<Interlinks>
		<Interlink id="iwb-interlink">
		    <LinkType>$linkType$</LinkType>
		
			<SourceDataset dataSource="local" var="$srcVar$">
				<RestrictTo>
					$srcRestrictTo$
				</RestrictTo>
			</SourceDataset>

			<TargetDataset dataSource="local" var="$targetVar$">
				<RestrictTo>
					$targetRestrictTo$
				</RestrictTo>
			</TargetDataset>

			<LinkageRule>
				$linkageRule$
			</LinkageRule>
			
			<Filter />
			
			<Outputs>
				<Output type="file" minConfidence="$filterThreshold$">
					<Param name="file" value="$outputFile$" />
					<Param name="format" value="ntriples" />
				</Output>
			</Outputs>
					
		</Interlink>
	</Interlinks>
</Silk>